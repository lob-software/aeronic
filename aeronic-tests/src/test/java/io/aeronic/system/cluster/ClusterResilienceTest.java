package io.aeronic.system.cluster;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.cluster.service.Cluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeronic.AeronicWizard;
import io.aeronic.SimpleEvents;
import io.aeronic.cluster.AeronicClusteredServiceContainer;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static io.aeronic.Assertions.assertEventuallyTrue;
import static io.aeronic.system.cluster.TestClusterNode.Service;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ClusterResilienceTest
{
    private static final int STREAM_ID = 101;

    private static final String MDC_CAST_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .controlEndpoint("localhost:40458")
        .controlMode("dynamic")
        .endpoint("localhost:40459")
        .build();

    private AeronicWizard aeronic;
    private Aeron aeron;
    private MediaDriver mediaDriver;
    private final TestCluster testCluster = new TestCluster();

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
        aeronic = new AeronicWizard(aeron);

        testCluster.registerNode(0, 3);
        testCluster.registerNode(1, 3);
        testCluster.registerNode(2, 3);
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

        // register as normal (non-testCluster) subs
        aeronic.registerSubscriber(SimpleEvents.class, sub1, MDC_CAST_CHANNEL, STREAM_ID);
        aeronic.registerSubscriber(SimpleEvents.class, sub2, MDC_CAST_CHANNEL, STREAM_ID);

        aeronic.start();

        // wait for leader, publish from it and assert that only leaders messages get published
        final AeronicClusteredServiceContainer leaderClusteredService = testCluster.waitForLeader();
        final SimpleEvents leaderPublisher = leaderClusteredService.getMultiplexingPublisherFor(SimpleEvents.class);
        assertEventuallyTrue(leaderClusteredService::egressConnected);
        testCluster.forEachNonLeaderNode(node -> assertFalse(node.egressConnected()));

        leaderPublisher.onEvent(101L);
        testCluster.forEachNonLeaderNode(node -> {
            final SimpleEvents publisher = node.getMultiplexingPublisherFor(SimpleEvents.class);
            publisher.onEvent(303L);
        });

        // follower egress publish should be noop
        assertEventuallyTrue(() -> sub1.value == 101L && sub2.value == 101L);
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

    private static class TestCluster
    {
        private final List<AeronicClusteredServiceContainer> clusteredServices = new ArrayList<>();
        private final List<TestClusterNode> clusterNodes = new ArrayList<>();

        public void registerNode(final int nodeIdx, final int nodeCount)
        {
            final AeronicClusteredServiceContainer clusteredServiceContainer = new AeronicClusteredServiceContainer(
                new AeronicClusteredServiceContainer.Configuration()
                    .clusteredService(new Service())
                    .registerMultiplexingEgressPublisher(SimpleEvents.class, MDC_CAST_CHANNEL, STREAM_ID)
            );

            final TestClusterNode clusterNode = new TestClusterNode(nodeIdx, nodeCount, clusteredServiceContainer);

            clusteredServices.add(clusteredServiceContainer);
            clusterNodes.add(clusterNode);
        }

        public AeronicClusteredServiceContainer waitForLeader()
        {
            final Optional<AeronicClusteredServiceContainer> leaderMaybe = await()
                .until(
                    () -> clusteredServices.stream()
                        .filter(e -> e.getRole() == Cluster.Role.LEADER)
                        .findFirst(),
                    Optional::isPresent
                );

            return leaderMaybe.orElse(null);
        }

        public void forEachNonLeaderNode(final Consumer<? super AeronicClusteredServiceContainer> consumer)
        {
            clusteredServices.stream()
                .filter(e -> e.getRole() != Cluster.Role.LEADER)
                .forEach(consumer);
        }

        public void close()
        {
            clusterNodes.forEach(TestClusterNode::close);
            clusteredServices.forEach(AeronicClusteredServiceContainer::close);
        }
    }
}
