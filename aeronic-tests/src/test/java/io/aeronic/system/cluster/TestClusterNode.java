package io.aeronic.system.cluster;


import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.cluster.ClusteredMediaDriver;
import io.aeron.cluster.ConsensusModule;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.cluster.client.EgressListener;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import io.aeron.cluster.service.ClusteredServiceContainer;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.logbuffer.Header;
import io.aeron.security.Authenticator;
import io.aeron.security.CredentialsSupplier;
import io.aeron.security.SessionProxy;
import io.aeronic.AeronicWizard;
import io.aeronic.SampleEvents;
import io.aeronic.SimpleEvents;
import io.aeronic.net.AbstractSubscriberInvoker;
import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.ErrorHandler;
import org.agrona.collections.ArrayUtil;
import org.agrona.concurrent.IdleStrategy;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public final class TestClusterNode implements AutoCloseable
{
    private static final String INGRESS_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .endpoint("localhost:40457")
        .build();

    private static final String REPLICATION_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .endpoint("localhost:40458")
        .build();

    // cluster side
    private final ClusteredMediaDriver clusteredMediaDriver;
    private final ClusteredServiceContainer container;

    // cluster client side
    private MediaDriver clientMediaDriver;

    private final EgressListener egressMessageListener = new EgressListener()
    {
        public void onMessage(
            final long clusterSessionId,
            final long timestamp,
            final DirectBuffer buffer,
            final int offset,
            final int length,
            final Header header
        )
        {
            System.out.println("egress onMessage " + clusterSessionId);
        }

        public void onNewLeader(
            final long clusterSessionId,
            final long leadershipTermId,
            final int leaderMemberId,
            final String ingressEndpoints
        )
        {
            System.out.println("TestClusterNode.onNewLeader");
        }
    };

    static class Service implements ClusteredService
    {
        protected Cluster cluster;
        protected IdleStrategy idleStrategy;

        private int messageCount = 0;
        private final Map<Long, AbstractSubscriberInvoker<?>> subscriberBySessionId = new HashMap<>();
        private final Map<String, AbstractSubscriberInvoker<?>> invokerByClassName = new HashMap<>();

        public Service(final ClusterSystemTest.SimpleEventsImpl simpleEvents, final ClusterSystemTest.SampleEventsImpl sampleEvents)
        {
            invokerByClassName.put(SimpleEvents.class.getName(), AeronicWizard.createSubscriberInvoker(SimpleEvents.class, simpleEvents));
            invokerByClassName.put(SampleEvents.class.getName(), AeronicWizard.createSubscriberInvoker(SampleEvents.class, sampleEvents));
        }

        public void onStart(final Cluster cluster, final Image snapshotImage)
        {
            this.cluster = cluster;
            this.idleStrategy = cluster.idleStrategy();
        }

        public void onSessionOpen(final ClientSession session, final long timestamp)
        {
            System.out.println("onSessionOpen " + session.id());

            final byte[] encodedPrincipal = session.encodedPrincipal();
            if (encodedPrincipal.length != 0)
            {
                final String subscriberClassName = new String(encodedPrincipal);
                subscriberBySessionId.put(session.id(), invokerByClassName.get(subscriberClassName));
            }
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
            subscriberBySessionId.get(session.id()).handle(buffer, offset);
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

    public TestClusterNode(final ClusteredService clusteredService, final boolean isCleanStart)
    {
        final String aeronDirectoryName = CommonContext.getAeronDirectoryName() + "-node";

        final MediaDriver.Context mediaDriverContext = new MediaDriver.Context();
        final ConsensusModule.Context consensusModuleContext = new ConsensusModule.Context();
        final Archive.Context archiveContext = new Archive.Context();
        final ClusteredServiceContainer.Context serviceContainerContext = new ClusteredServiceContainer.Context();

        mediaDriverContext
            .aeronDirectoryName(aeronDirectoryName)
            .threadingMode(ThreadingMode.SHARED)
            .errorHandler(Throwable::printStackTrace)
            .dirDeleteOnShutdown(true)
            .dirDeleteOnStart(true);

        archiveContext
            .archiveDirectoryName(aeronDirectoryName + "-archive")
            .deleteArchiveOnStart(isCleanStart)
            .recordingEventsEnabled(false)
            .threadingMode(ArchiveThreadingMode.SHARED);

        final String clusterDirectoryName = aeronDirectoryName + "-cluster";

        consensusModuleContext
            .authenticatorSupplier(SimpleAuthenticator::new)
            .clusterDirectoryName(clusterDirectoryName)
            .aeronDirectoryName(aeronDirectoryName)
            .ingressChannel(INGRESS_CHANNEL)
            .replicationChannel(REPLICATION_CHANNEL)
            .errorHandler(Throwable::printStackTrace)
            .deleteDirOnStart(isCleanStart);

        serviceContainerContext
            .clusterDirectoryName(clusterDirectoryName)
            .aeronDirectoryName(aeronDirectoryName)
            .clusteredService(clusteredService)
            .errorHandler(Throwable::printStackTrace);

        clusteredMediaDriver = ClusteredMediaDriver.launch(
            mediaDriverContext,
            archiveContext,
            consensusModuleContext
        );

        container = ClusteredServiceContainer.launch(serviceContainerContext);
    }

    public void close()
    {
        final ErrorHandler errorHandler = clusteredMediaDriver.mediaDriver().context().errorHandler();
        CloseHelper.close(errorHandler, clientMediaDriver);
        CloseHelper.close(errorHandler, clusteredMediaDriver.consensusModule());
        CloseHelper.close(errorHandler, container);
        CloseHelper.close(clusteredMediaDriver); // ErrorHandler will be closed during that call so can't use it
    }

    AeronCluster connectClientToCluster(final String clientName)
    {
        final String aeronDirectoryName = CommonContext.getAeronDirectoryName() + "-" + clientName;

        clientMediaDriver = MediaDriver.launch(
            new MediaDriver.Context()
                .aeronDirectoryName(aeronDirectoryName)
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true)
                .errorHandler(Throwable::printStackTrace));

        return AeronCluster.connect(
            new AeronCluster.Context()
                .credentialsSupplier(new SimpleCredentialsSupplier(clientName))
                .ingressChannel(INGRESS_CHANNEL)
                .errorHandler(Throwable::printStackTrace)
                .egressListener(egressMessageListener)
                .aeronDirectoryName(aeronDirectoryName));
    }

    private static class SimpleCredentialsSupplier implements CredentialsSupplier
    {
        private final String name;

        public SimpleCredentialsSupplier(String name)
        {
            this.name = name;
        }

        @Override
        public byte[] encodedCredentials()
        {
            return name.getBytes();
        }

        @Override
        public byte[] onChallenge(byte[] encodedChallenge)
        {
            return ArrayUtil.EMPTY_BYTE_ARRAY;
        }
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
