package io.aeronic.system.cluster;


import io.aeron.Aeron;
import io.aeron.CommonContext;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.client.AeronArchive;
import io.aeron.cluster.ClusteredMediaDriver;
import io.aeron.cluster.ConsensusModule;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.logbuffer.Header;
import io.aeron.security.Authenticator;
import io.aeron.security.SessionProxy;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.IoUtil;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.NoOpLock;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.aeronic.system.cluster.ClusterUtil.archiveControlRequestChannel;
import static io.aeronic.system.cluster.ClusterUtil.clusterMembers;


public final class TestClusterNode implements AutoCloseable
{
    private static final long CATALOG_CAPACITY = 128 * 1024;
    private static final String ARCHIVE_LOCAL_CONTROL_CHANNEL = "aeron:ipc";
    private static final long STARTUP_CANVASS_TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);
    public static final String INGRESS_CHANNEL = "aeron:udp?term-length=128k|alias=ingress";

    private static final String LOG_CHANNEL = "aeron:udp?term-length=512k";
    private static final String ARCHIVE_CONTROL_RESPONSE_CHANNEL = "aeron:udp?endpoint=localhost:0";
    private static final String REPLICATION_CHANNEL = "aeron:udp?endpoint=localhost:0";

    public static final String LOCALHOST = "localhost";

    private final ClusteredMediaDriver clusteredMediaDriver;
    private final ClusteredServiceContainer container;
    private final String aeronDirName;
    private final String baseDirName;
    private final String clusterDirectoryName;
    private final File clusterDir;

    public static class Context
    {
        private int nodeId;
        private int nodeCount;
        private String ingressChannel;
        private ClusteredService clusteredService;
        private boolean deleteDirs;

        public Context nodeId(final int nodeId)
        {
            this.nodeId = nodeId;
            return this;
        }

        public Context nodeCount(final int nodeCount)
        {
            this.nodeCount = nodeCount;
            return this;
        }

        public Context ingressChannel(final String ingressChannel)
        {
            this.ingressChannel = ingressChannel;
            return this;
        }

        public Context clusteredService(final ClusteredService clusteredService)
        {
            this.clusteredService = clusteredService;
            return this;
        }

        public Context deleteDirs(final boolean deleteDirs)
        {
            this.deleteDirs = deleteDirs;
            return this;
        }
    }

    public static TestClusterNode startNodeOnIngressChannel(
        final int nodeId,
        final int nodeCount,
        final ClusteredService clusteredService,
        final String ingressChannel
    )
    {
        return new TestClusterNode(
            new Context()
                .nodeId(nodeId)
                .nodeCount(nodeCount)
                .clusteredService(clusteredService)
                .ingressChannel(ingressChannel)
                .deleteDirs(true));
    }

    public TestClusterNode(final int nodeId, final int nodeCount, final ClusteredService clusteredService)
    {
        this(
            new Context()
                .nodeId(nodeId)
                .nodeCount(nodeCount)
                .clusteredService(clusteredService)
                .ingressChannel(INGRESS_CHANNEL)
                .deleteDirs(true));
    }

    public TestClusterNode(final Context ctx)
    {
        final int nodeId = ctx.nodeId;
        final int nodeCount = ctx.nodeCount;
        final boolean deleteDirs = ctx.deleteDirs;

        this.aeronDirName = CommonContext.getAeronDirectoryName() + "-" + nodeId + "-driver";
        this.baseDirName = CommonContext.getAeronDirectoryName() + "-" + nodeId;
        this.clusterDirectoryName = aeronDirName + "-cluster-" + nodeId;

        final MediaDriver.Context mediaDriverContext = new MediaDriver.Context();
        final ConsensusModule.Context consensusModuleContext = new ConsensusModule.Context();
        final Archive.Context archiveContext = new Archive.Context();
        final AeronArchive.Context aeronArchiveContext = new AeronArchive.Context();
        final ClusteredServiceContainer.Context serviceContainerContext = new ClusteredServiceContainer.Context();

        aeronArchiveContext
            .lock(NoOpLock.INSTANCE)
            .controlRequestChannel(archiveControlRequestChannel(nodeId))
            .controlResponseChannel(ARCHIVE_CONTROL_RESPONSE_CHANNEL)
            .aeronDirectoryName(aeronDirName);

        mediaDriverContext
            .aeronDirectoryName(aeronDirName)
            .threadingMode(ThreadingMode.SHARED)
            .termBufferSparseFile(true)
            .dirDeleteOnShutdown(deleteDirs)
            .dirDeleteOnStart(deleteDirs);

        archiveContext
            .replicationChannel("aeron:ipc")
            .catalogCapacity(CATALOG_CAPACITY)
            .archiveDir(new File(baseDirName, "archive"))
            .controlChannel(aeronArchiveContext.controlRequestChannel())
            .localControlChannel(ARCHIVE_LOCAL_CONTROL_CHANNEL)
            .recordingEventsEnabled(false)
            .threadingMode(ArchiveThreadingMode.SHARED)
            .deleteArchiveOnStart(deleteDirs);

        clusterDir = new File(baseDirName, "consensus-module");

        consensusModuleContext
            .errorHandler(Throwable::printStackTrace)
            .clusterMemberId(nodeId)
            .clusterMembers(clusterMembers(0, nodeCount))
            .startupCanvassTimeoutNs(STARTUP_CANVASS_TIMEOUT_NS)
            .appointedLeaderId(Aeron.NULL_VALUE)
            .clusterDir(clusterDir)
            .ingressChannel(ctx.ingressChannel)
            .logChannel(LOG_CHANNEL)
            .replicationChannel(REPLICATION_CHANNEL)
            .archiveContext(aeronArchiveContext.clone()
                .controlRequestChannel(ARCHIVE_LOCAL_CONTROL_CHANNEL)
                .controlResponseChannel(ARCHIVE_LOCAL_CONTROL_CHANNEL))
            .sessionTimeoutNs(TimeUnit.SECONDS.toNanos(10))
            .leaderHeartbeatIntervalNs(TimeUnit.SECONDS.toNanos(1))
            .leaderHeartbeatTimeoutNs(TimeUnit.SECONDS.toNanos(2))
            .authenticatorSupplier(SimpleAuthenticator::new)
            .deleteDirOnStart(deleteDirs);

        serviceContainerContext
            .aeronDirectoryName(aeronDirName)
            .clusterDirectoryName(clusterDirectoryName)
            .clusteredService(ctx.clusteredService)
            .errorHandler(Throwable::printStackTrace);

        clusteredMediaDriver = ClusteredMediaDriver.launch(
            mediaDriverContext,
            archiveContext,
            consensusModuleContext
        );

        container = ClusteredServiceContainer.launch(serviceContainerContext);
    }

    static class Service implements ClusteredService
    {
        protected Cluster cluster;
        protected IdleStrategy idleStrategy;
        private int messageCount = 0;

        public int getMessageCount()
        {
            return messageCount;
        }

        public void onStart(final Cluster cluster, final Image snapshotImage)
        {
            this.cluster = cluster;
            this.idleStrategy = cluster.idleStrategy();
            System.out.println("Node ID: " + cluster.memberId() + " onStart " + cluster.role());
        }

        public void onSessionOpen(final ClientSession session, final long timestamp)
        {
            System.out.println("onSessionOpen " + session.id());
        }

        public void onSessionMessage(
            final ClientSession session,
            final long timestamp,
            final DirectBuffer buffer,
            final int offset,
            final int length,
            final Header header
        )
        {
            messageCount++;
            System.out.println(cluster.role() + " onSessionMessage " + session.id() + " count=" + messageCount);
        }

        public void onSessionClose(final ClientSession session, final long timestamp, final CloseReason closeReason)
        {
            System.out.println("onSessionClose " + session.id() + " " + closeReason);
        }

        public void onTimerEvent(final long correlationId, final long timestamp)
        {
            System.out.println("onTimerEvent " + correlationId);
        }

        public void onTakeSnapshot(final ExclusivePublication snapshotPublication)
        {
            System.out.println("onTakeSnapshot messageCount=" + messageCount);
        }

        public void onRoleChange(final Cluster.Role newRole)
        {
            System.out.println("Node ID: " + cluster.memberId() + " onRoleChange " + newRole);
        }

        public void onTerminate(final Cluster cluster)
        {
            System.out.println("Node ID: " + cluster.memberId() + " onTerminate ");
        }

        public void onNewLeadershipTermEvent(
            final long leadershipTermId,
            final long logPosition,
            final long timestamp,
            final long termBaseLogPosition,
            final int leaderMemberId,
            final int logSessionId,
            final TimeUnit timeUnit,
            final int appVersion
        )
        {
            System.out.println("Node ID: " + cluster.memberId() + " onNewLeadershipTermEvent. I am: " + cluster.role() + " Log position: " + logPosition);
        }
    }

    public void close()
    {
        CloseHelper.closeAll(
            clusteredMediaDriver.consensusModule(),
            container,
            clusteredMediaDriver.archive(),
            clusteredMediaDriver
        );
    }

    public void deleteDirs()
    {
        IoUtil.delete(new File(aeronDirName), true);
        IoUtil.delete(new File(baseDirName), true);
        IoUtil.delete(new File(clusterDirectoryName), true);
    }

    private static class SimpleAuthenticator implements Authenticator
    {
        private final Map<Long, String> credentials = new HashMap<>();

        public void onConnectRequest(final long sessionId, final byte[] encodedCredentials, final long nowMs)
        {
            final String credentialsString = new String(encodedCredentials, StandardCharsets.US_ASCII);
            credentials.put(sessionId, credentialsString);
        }

        public void onChallengeResponse(final long sessionId, final byte[] encodedCredentials, final long nowMs)
        {
        }

        public void onConnectedSession(final SessionProxy sessionProxy, final long nowMs)
        {
            final String credentialsToEcho = credentials.get(sessionProxy.sessionId());
            if (null != credentialsToEcho)
            {
                sessionProxy.authenticate(credentialsToEcho.getBytes());
            }
        }

        public void onChallengedSession(final SessionProxy sessionProxy, final long nowMs)
        {
            final String credentialsToEcho = credentials.get(sessionProxy.sessionId());
            if (null != credentialsToEcho)
            {
                sessionProxy.authenticate(credentialsToEcho.getBytes());
            }
        }
    }
}
