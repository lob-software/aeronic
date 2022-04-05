package io.aeronic.system.cluster;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeronic.AeronicWizard;
import io.aeronic.SampleEvents;
import io.aeronic.SimpleEvents;
import io.aeronic.cluster.ClientSessionPublication;
import io.aeronic.cluster.EgressPublishers;
import io.aeronic.cluster.IngressSubscribers;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

public class ClusterSystemTest
{

    private static final String INGRESS_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .endpoint("localhost:40457")
        .build();

    private AeronicWizard aeronic;
    private Aeron aeron;
    private MediaDriver mediaDriver;
    private TestClusterNode clusterNode;
    private SimpleEventsImpl simpleEvents;
    private SampleEventsImpl sampleEvents;
    private TestClusterNode.Service clusteredService;

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
    }

    @AfterEach
    void tearDown()
    {
        aeronic.close();
        aeron.close();
        mediaDriver.close();
        clusterNode.close();
    }

    @Test
    public void clientToCluster()
    {
        simpleEvents = new SimpleEventsImpl();
        sampleEvents = new SampleEventsImpl();

        clusteredService = new TestClusterNode.Service(
            IngressSubscribers.create(
                AeronicWizard.createSubscriberInvoker(SimpleEvents.class, simpleEvents),
                AeronicWizard.createSubscriberInvoker(SampleEvents.class, sampleEvents)
            ),
            EgressPublishers.none()
        );

        clusterNode = new TestClusterNode(clusteredService, true);

        final SimpleEvents simpleEventsPublisher = aeronic.createClusterIngressPublisher(SimpleEvents.class, INGRESS_CHANNEL);
        final SampleEvents sampleEventsPublisher = aeronic.createClusterIngressPublisher(SampleEvents.class, INGRESS_CHANNEL);

        simpleEventsPublisher.onEvent(101L);
        sampleEventsPublisher.onEvent(201L);

        await()
            .timeout(Duration.ofSeconds(1))
            .until(() -> simpleEvents.value == 101L && sampleEvents.value == 201L && clusteredService.getMessageCount() == 2);
    }

    @Test
    public void clusterToClient()
    {
        simpleEvents = new SimpleEventsImpl();

        final ClientSessionPublication<SimpleEvents> simpleEventsPublisher = AeronicWizard.createClusterEgressPublisher(SimpleEvents.class);

        clusteredService = new TestClusterNode.Service(
            IngressSubscribers.none(),
            EgressPublishers.create(simpleEventsPublisher)
        );

        clusterNode = new TestClusterNode(clusteredService, true);

        final AeronCluster aeronCluster = aeronic.registerClusterEgressSubscriber(SimpleEvents.class, simpleEvents, INGRESS_CHANNEL); // egress channel will be different

        // TODO: poll on AeronicWizard's duty cycle
        new Thread(() -> pollCluster(aeronCluster)).start();

        await()
            .timeout(Duration.ofSeconds(1))
            .until(simpleEventsPublisher::isConnected);

        simpleEventsPublisher.getPublisher().onEvent(101L);

        await()
            .timeout(Duration.ofSeconds(1))
            .until(() -> simpleEvents.value == 101L);
    }

    private void pollCluster(final AeronCluster aeronCluster)
    {
        while (!aeronCluster.isClosed())
        {
            aeronCluster.pollEgress();
        }
    }

    @Test
    @Disabled
    public void clientToClusterInPresenceOfOtherPubsAndSubs()
    {

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
