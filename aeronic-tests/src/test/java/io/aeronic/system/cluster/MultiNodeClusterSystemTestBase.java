package io.aeronic.system.cluster;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeronic.Aeronic;
import io.aeronic.SimpleEvents;
import io.aeronic.cluster.AeronicClusteredServiceContainer;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.aeronic.Assertions.assertEventuallyTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public abstract class MultiNodeClusterSystemTestBase
{
    private static final int STREAM_ID = 101;

    private Aeronic aeronic;
    private Aeron aeron;
    private MediaDriver mediaDriver;
    private final TestCluster testCluster = new TestCluster();

    protected abstract String egressChannel();

    @BeforeEach
    void setUp()
    {
        mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
            .dirDeleteOnStart(true)
            .dirDeleteOnShutdown(true)
            .spiesSimulateConnection(true)
            .threadingMode(ThreadingMode.SHARED)
            .sharedIdleStrategy(new BusySpinIdleStrategy()));

        final Aeron.Context aeronCtx = new Aeron.Context()
            .aeronDirectoryName(mediaDriver.aeronDirectoryName());

        aeron = Aeron.connect(aeronCtx);
        aeronic = Aeronic.launch(new Aeronic.Context().aeron(aeron));

        testCluster.registerNode(0, 3, newClusteredServiceContainer());
        testCluster.registerNode(1, 3, newClusteredServiceContainer());
        testCluster.registerNode(2, 3, newClusteredServiceContainer());
    }

    private AeronicClusteredServiceContainer newClusteredServiceContainer()
    {
        return new AeronicClusteredServiceContainer(
            new AeronicClusteredServiceContainer.Configuration()
                .clusteredService(new TestClusterNode.Service())
                .registerToggledEgressPublisher(SimpleEvents.class, egressChannel(), STREAM_ID)
        );
    }

    @AfterEach
    void tearDown()
    {
        aeronic.close();
        aeron.close();
        mediaDriver.close();
        testCluster.close();
    }

    @Test
    public void onlyLeaderCanPublish()
    {
        final SimpleEventsImpl sub1 = new SimpleEventsImpl();
        final SimpleEventsImpl sub2 = new SimpleEventsImpl();

        // register as normal (non-cluster) aeron subs
        aeronic.registerSubscriber(SimpleEvents.class, sub1, egressChannel(), STREAM_ID);
        aeronic.registerSubscriber(SimpleEvents.class, sub2, egressChannel(), STREAM_ID);

        // wait for leader, publish from it and assert that only leaders messages get published
        final AeronicClusteredServiceContainer leaderClusteredService = testCluster.waitForLeader();
        final SimpleEvents leaderPublisher = leaderClusteredService.getToggledPublisherFor(SimpleEvents.class);
        assertEventuallyTrue(leaderClusteredService::egressConnected);
        testCluster.forEachNonLeaderNode(node -> assertFalse(node.egressConnected()));

        leaderPublisher.onEvent(101L);
        testCluster.forEachNonLeaderNode(node -> {
            final SimpleEvents publisher = node.getToggledPublisherFor(SimpleEvents.class);
            publisher.onEvent(303L);
        });

        // follower egress publish should be noop
        assertEventuallyTrue(() -> sub1.value == 101L && sub2.value == 101L);
    }

    @Test
    public void newLeaderCanPublishAfterFailover()
    {
        final SimpleEventsImpl sub1 = new SimpleEventsImpl();
        final SimpleEventsImpl sub2 = new SimpleEventsImpl();

        // register as normal (non-cluster) aeron subs
        aeronic.registerSubscriber(SimpleEvents.class, sub1, egressChannel(), STREAM_ID);
        aeronic.registerSubscriber(SimpleEvents.class, sub2, egressChannel(), STREAM_ID);

        final AeronicClusteredServiceContainer leaderClusteredService = testCluster.waitForLeader();
        assertEventuallyTrue(leaderClusteredService::egressConnected);

        final AeronicClusteredServiceContainer newLeader = testCluster.shutdownLeader();
        assertNotSame(leaderClusteredService, newLeader);

        final SimpleEvents newLeaderPublisher = newLeader.getToggledPublisherFor(SimpleEvents.class);
        assertEventuallyTrue(newLeader::egressConnected);

        newLeaderPublisher.onEvent(101L);
        testCluster.forEachNonLeaderNode(node -> {
            final SimpleEvents publisher = node.getToggledPublisherFor(SimpleEvents.class);
            publisher.onEvent(303L);
        });

        assertEventuallyTrue(() -> sub1.value == 101L && sub2.value == 101L, 5000);
    }

    @Test
    public void leaderRestartedAsFollowerCannotPublish()
    {
        final SimpleEventsImpl sub1 = new SimpleEventsImpl();
        final SimpleEventsImpl sub2 = new SimpleEventsImpl();

        // register as normal (non-cluster) aeron subs
        aeronic.registerSubscriber(SimpleEvents.class, sub1, egressChannel(), STREAM_ID);
        aeronic.registerSubscriber(SimpleEvents.class, sub2, egressChannel(), STREAM_ID);

        final AeronicClusteredServiceContainer leaderClusteredService = testCluster.waitForLeader();
        final int leaderIdx = testCluster.getNodeIdx(leaderClusteredService);
        assertEventuallyTrue(leaderClusteredService::egressConnected);

        final AeronicClusteredServiceContainer newLeader = testCluster.shutdownLeader();
        final SimpleEvents newLeaderPublisher = newLeader.getToggledPublisherFor(SimpleEvents.class);
        assertEventuallyTrue(newLeader::egressConnected);

        // reintroduce node, starting it from existing logs
        final AeronicClusteredServiceContainer oldLeader = testCluster.restartNode(leaderIdx, 3);
        final SimpleEvents oldLeaderPublisher = oldLeader.getToggledPublisherFor(SimpleEvents.class);

        newLeaderPublisher.onEvent(101L);
        oldLeaderPublisher.onEvent(202L);

        assertEventuallyTrue(() -> sub1.value == 101L && sub2.value == 101L, 5000);
    }

    public static class SimpleEventsImpl implements SimpleEvents
    {

        private volatile long value;

        @Override
        public void onEvent(final long value)
        {
            this.value = value;
        }
    }
}
