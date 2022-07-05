package io.aeronic.system.cluster;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeronic.AeronicWizard;
import io.aeronic.SampleEvents;
import io.aeronic.SimpleEvents;
import io.aeronic.cluster.AeronicClusteredServiceContainer;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

@Disabled("WIP. AeronClusterPublication::pollCluster must be necessary during failover")
public class ClusterResilienceTest
{

    private static final String INGRESS_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .endpoint("localhost:40457")
        .build();

    private AeronicWizard aeronic;
    private Aeron aeron;
    private MediaDriver mediaDriver;

    // node1
    private TestClusterNode clusterNode1;
    private AeronicClusteredServiceContainer clusteredService1;

    // node2
    private TestClusterNode clusterNode2;
    private AeronicClusteredServiceContainer clusteredService2;

    // node3
    private TestClusterNode clusterNode3;
    private AeronicClusteredServiceContainer clusteredService3;

    private SimpleEventsImpl simpleEvents;
    private SampleEventsImpl sampleEvents;

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

        simpleEvents = new SimpleEventsImpl();
        sampleEvents = new SampleEventsImpl();
    }

    @AfterEach
    void tearDown()
    {
        aeronic.close();
        aeron.close();
        mediaDriver.close();
        clusterNode1.close();
        clusteredService1.close();
    }

    @Test
    public void clusterIngressAndEgress()
    {
        final SimpleEventsImpl clusterIngressSimpleEventsImpl = new SimpleEventsImpl();

        final TestClusterNode.Service service = new TestClusterNode.Service();

        clusteredService1 = new AeronicClusteredServiceContainer(
            new AeronicClusteredServiceContainer.Configuration()
                .clusteredService(service)
                .registerIngressSubscriber(SimpleEvents.class, clusterIngressSimpleEventsImpl)
                .registerEgressPublisher(SampleEvents.class));

        final SampleEvents clusterEgressSampleEventsPublisher = clusteredService1.getPublisherFor(SampleEvents.class);

        clusterNode1 = new TestClusterNode(clusteredService1, true);

        final SimpleEvents clusterIngressSimpleEventsPublisher = aeronic.createClusterIngressPublisher(SimpleEvents.class, INGRESS_CHANNEL);

        aeronic.registerClusterEgressSubscriber(SampleEvents.class, sampleEvents, INGRESS_CHANNEL);
        aeronic.start();
        aeronic.awaitUntilPubsAndSubsConnect();
        await().timeout(Duration.ofSeconds(1)).until(clusteredService1::egressConnected);

        // cluster -> client
        clusterEgressSampleEventsPublisher.onEvent(202L);

        // client -> cluster
        clusterIngressSimpleEventsPublisher.onEvent(303L);

        await()
            .timeout(Duration.ofSeconds(1))
            .until(() ->
                simpleEvents.value == 101L &&
                    sampleEvents.value == 202L &&
                    clusterIngressSimpleEventsImpl.value == 303L);
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

    public static class SampleEventsImpl implements SampleEvents
    {

        private volatile long value;

        @Override
        public void onEvent(final long value)
        {
            this.value = value;
        }
    }
}
