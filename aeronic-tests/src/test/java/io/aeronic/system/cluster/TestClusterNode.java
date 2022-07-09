package io.aeronic.system.cluster;


import io.aeron.*;
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
import org.agrona.ErrorHandler;
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
    public static final String INGRESS_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .endpoint("localhost:40457")
        .build();

    private static final String LOG_CHANNEL = "aeron:udp?term-length=512k";
    private static final String ARCHIVE_CONTROL_RESPONSE_CHANNEL = "aeron:udp?endpoint=localhost:0";

    private static final String REPLICATION_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .endpoint("localhost:40458")
        .build();

    public static final String LOCALHOST = "localhost";

    private final ClusteredMediaDriver clusteredMediaDriver;
    private final ClusteredServiceContainer container;

    public TestClusterNode(final int nodeId, final int nodeCount, final ClusteredService clusteredService)
    {
        final String aeronDirName = CommonContext.getAeronDirectoryName() + "-" + nodeId + "-driver";
        final String baseDirName = CommonContext.getAeronDirectoryName() + "-" + nodeId;

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
            .dirDeleteOnShutdown(true)
            .dirDeleteOnStart(true);

        archiveContext
            .catalogCapacity(CATALOG_CAPACITY)
            .archiveDir(new File(baseDirName, "archive"))
            .controlChannel(aeronArchiveContext.controlRequestChannel())
            .localControlChannel(ARCHIVE_LOCAL_CONTROL_CHANNEL)
            .recordingEventsEnabled(false)
            .recordingEventsEnabled(false)
            .threadingMode(ArchiveThreadingMode.SHARED)
            .deleteArchiveOnStart(true);

        consensusModuleContext
            .clusterMemberId(nodeId)
            .clusterMembers(clusterMembers(0, nodeCount))
            .startupCanvassTimeoutNs(STARTUP_CANVASS_TIMEOUT_NS)
            .appointedLeaderId(Aeron.NULL_VALUE)
            .clusterDir(new File(baseDirName, "consensus-module"))
            .ingressChannel(INGRESS_CHANNEL)
            .logChannel(LOG_CHANNEL)
            .replicationChannel(REPLICATION_CHANNEL)
            .archiveContext(aeronArchiveContext.clone()
                .controlRequestChannel(ARCHIVE_LOCAL_CONTROL_CHANNEL)
                .controlResponseChannel(ARCHIVE_LOCAL_CONTROL_CHANNEL))
            .sessionTimeoutNs(TimeUnit.SECONDS.toNanos(10))
            .authenticatorSupplier(SimpleAuthenticator::new)
            .deleteDirOnStart(true);

        serviceContainerContext
            .aeronDirectoryName(aeronDirName)
            .clusteredService(clusteredService)
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
            System.out.println("onRoleChange " + newRole);
        }

        public void onTerminate(final Cluster cluster)
        {
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
            System.out.println("onNewLeadershipTermEvent");
        }
    }

    public void close()
    {
        final ErrorHandler errorHandler = clusteredMediaDriver.mediaDriver().context().errorHandler();
        CloseHelper.close(errorHandler, clusteredMediaDriver.consensusModule());
        CloseHelper.close(errorHandler, container);
        CloseHelper.close(clusteredMediaDriver); // ErrorHandler will be closed during that call so can't use it
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
