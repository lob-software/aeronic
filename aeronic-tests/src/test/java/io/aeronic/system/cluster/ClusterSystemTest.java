package io.aeronic.system.cluster;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeronic.AeronicImpl;
import io.aeronic.SampleEvents;
import io.aeronic.SimpleEvents;
import io.aeronic.cluster.AeronicClusteredServiceContainer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.aeronic.Assertions.assertEventuallyTrue;
import static io.aeronic.system.cluster.TestClusterNode.startNodeOnIngressChannel;

public class ClusterSystemTest
{
    private static final String INGRESS_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .endpoint("localhost:40457")
        .build();

    private static final String MDC_CAST_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .controlEndpoint("localhost:40458")
        .controlMode("dynamic")
        .endpoint("localhost:40459")
        .build();

    private static final String UDP_MULTICAST_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .endpoint("224.0.1.1:40457")
        .networkInterface("localhost")
        .build();

    private AeronicImpl aeronic;
    private Aeron aeron;
    private MediaDriver mediaDriver;
    private TestClusterNode clusterNode;
    private SimpleEventsImpl simpleEvents;
    private SampleEventsImpl sampleEvents;
    private AeronicClusteredServiceContainer clusteredService;

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
        aeronic = AeronicImpl.launch(new AeronicImpl.Context().aeron(aeron));

        simpleEvents = new SimpleEventsImpl();
        sampleEvents = new SampleEventsImpl();
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
    public void clusterIngress()
    {
        final TestClusterNode.Service service = new TestClusterNode.Service();

        clusteredService = new AeronicClusteredServiceContainer(
            new AeronicClusteredServiceContainer.Configuration()
                .clusteredService(service)
                .registerIngressSubscriber(SimpleEvents.class, simpleEvents)
                .registerIngressSubscriber(SampleEvents.class, sampleEvents)
        );

        clusterNode = startNodeOnIngressChannel(0, 1, clusteredService, INGRESS_CHANNEL);

        final SimpleEvents simpleEventsPublisher = aeronic.createClusterIngressPublisher(SimpleEvents.class, INGRESS_CHANNEL);
        final SampleEvents sampleEventsPublisher = aeronic.createClusterIngressPublisher(
            SampleEvents.class,
            new AeronCluster.Context()
                .errorHandler(Throwable::printStackTrace)
                .ingressChannel(INGRESS_CHANNEL)
                .aeronDirectoryName(aeron.context().aeronDirectoryName())
        );

        aeronic.awaitUntilPubsAndSubsConnect();

        simpleEventsPublisher.onEvent(101L);
        sampleEventsPublisher.onEvent(201L);

        assertEventuallyTrue(() -> simpleEvents.value == 101L && sampleEvents.value == 201L && service.getMessageCount() == 2);
    }

    @Test
    public void clusterEgress()
    {
        final TestClusterNode.Service service = new TestClusterNode.Service();

        final AeronicClusteredServiceContainer.Configuration configuration = new AeronicClusteredServiceContainer.Configuration()
            .clusteredService(service)
            .registerEgressPublisher(SimpleEvents.class)
            .registerEgressPublisher(SampleEvents.class);

        final SimpleEvents simpleEventsPublisher = configuration.registry().getPublisherFor(SimpleEvents.class);
        final SampleEvents sampleEventsPublisher = configuration.registry().getPublisherFor(SampleEvents.class);

        clusteredService = new AeronicClusteredServiceContainer(configuration);
        clusterNode = startNodeOnIngressChannel(0, 1, clusteredService, INGRESS_CHANNEL);

        aeronic.registerClusterEgressSubscriber(SimpleEvents.class, simpleEvents, new AeronCluster.Context()
            .aeronDirectoryName(aeron.context().aeronDirectoryName())
            .errorHandler(Throwable::printStackTrace)
            .ingressChannel(INGRESS_CHANNEL));

        aeronic.registerClusterEgressSubscriber(SampleEvents.class, sampleEvents, INGRESS_CHANNEL);
        aeronic.awaitUntilPubsAndSubsConnect();
        assertEventuallyTrue(clusteredService::egressConnected);

        simpleEventsPublisher.onEvent(101L);
        sampleEventsPublisher.onEvent(202L);

        assertEventuallyTrue(() -> simpleEvents.value == 101L && sampleEvents.value == 202L);
    }

    @Test
    public void clusterIngressAndEgress()
    {
        final SimpleEventsImpl clusterIngressSimpleEventsImpl = new SimpleEventsImpl();
        final SampleEventsImpl clusterIngressSampleEventsImpl = new SampleEventsImpl();

        final TestClusterNode.Service service = new TestClusterNode.Service();

        clusteredService = new AeronicClusteredServiceContainer(
            new AeronicClusteredServiceContainer.Configuration()
                .clusteredService(service)
                .registerIngressSubscriber(SimpleEvents.class, clusterIngressSimpleEventsImpl)
                .registerIngressSubscriber(SampleEvents.class, clusterIngressSampleEventsImpl)
                .registerEgressPublisher(SimpleEvents.class)
                .registerEgressPublisher(SampleEvents.class));

        final SimpleEvents clusterEgressSimpleEventsPublisher = clusteredService.getPublisherFor(SimpleEvents.class);
        final SampleEvents clusterEgressSampleEventsPublisher = clusteredService.getPublisherFor(SampleEvents.class);

        clusterNode = startNodeOnIngressChannel(0, 1, clusteredService, INGRESS_CHANNEL);

        final SimpleEvents clusterIngressSimpleEventsPublisher = aeronic.createClusterIngressPublisher(SimpleEvents.class, INGRESS_CHANNEL);
        final SampleEvents clusterIngressSampleEventsPublisher = aeronic.createClusterIngressPublisher(SampleEvents.class, INGRESS_CHANNEL);

        aeronic.registerClusterEgressSubscriber(SimpleEvents.class, simpleEvents, INGRESS_CHANNEL);
        aeronic.registerClusterEgressSubscriber(SampleEvents.class, sampleEvents, INGRESS_CHANNEL);
        aeronic.awaitUntilPubsAndSubsConnect();
        assertEventuallyTrue(clusteredService::egressConnected);

        // cluster -> client
        clusterEgressSimpleEventsPublisher.onEvent(101L);
        clusterEgressSampleEventsPublisher.onEvent(202L);


        // client -> cluster
        clusterIngressSimpleEventsPublisher.onEvent(303L);
        clusterIngressSampleEventsPublisher.onEvent(404L);

        assertEventuallyTrue(() -> simpleEvents.value == 101L &&
            sampleEvents.value == 202L &&
            clusterIngressSimpleEventsImpl.value == 303L &&
            clusterIngressSampleEventsImpl.value == 404L);
    }

    @Test
    public void emptyClusteredServiceContainer()
    {
        final TestClusterNode.Service service = new TestClusterNode.Service();

        clusteredService = new AeronicClusteredServiceContainer(
            new AeronicClusteredServiceContainer.Configuration()
                .clusteredService(service));

        clusterNode = startNodeOnIngressChannel(0, 1, clusteredService, INGRESS_CHANNEL);

        final AeronCluster anotherClient = AeronCluster.connect(
            new AeronCluster.Context()
                .ingressChannel(INGRESS_CHANNEL)
                .aeronDirectoryName(aeron.context().aeronDirectoryName())
                .errorHandler(Throwable::printStackTrace));

        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer();
        buffer.putLong(0, 100);

        anotherClient.offer(buffer, 0, buffer.capacity());

        assertEventuallyTrue(() -> service.getMessageCount() == 1);

        anotherClient.close();
    }

    @Test
    public void sameIngressChannelPublishing()
    {
        final TestClusterNode.Service service = new TestClusterNode.Service();

        clusteredService = new AeronicClusteredServiceContainer(
            new AeronicClusteredServiceContainer.Configuration()
                .clusteredService(service)
                .registerIngressSubscriber(SimpleEvents.class, simpleEvents));

        clusterNode = startNodeOnIngressChannel(0, 1, clusteredService, INGRESS_CHANNEL);

        final SimpleEvents simpleEventsPublisher1 = aeronic.createClusterIngressPublisher(SimpleEvents.class, INGRESS_CHANNEL);
        final SimpleEvents simpleEventsPublisher2 = aeronic.createClusterIngressPublisher(SimpleEvents.class, INGRESS_CHANNEL);

        aeronic.awaitUntilPubsAndSubsConnect();

        simpleEventsPublisher1.onEvent(101L);
        assertEventuallyTrue(() -> simpleEvents.value == 101L && service.getMessageCount() == 1);

        simpleEventsPublisher2.onEvent(201L);
        assertEventuallyTrue(() -> simpleEvents.value == 201L && service.getMessageCount() == 2);
    }

    @Test
    public void sameEgressChannelPublishing()
    {
        final TestClusterNode.Service service = new TestClusterNode.Service();

        clusteredService = new AeronicClusteredServiceContainer(
            new AeronicClusteredServiceContainer.Configuration()
                .clusteredService(service)
                .registerEgressPublisher(SimpleEvents.class));

        final SimpleEvents clusterEgressSimpleEventsPublisher = clusteredService.getPublisherFor(SimpleEvents.class);

        clusterNode = startNodeOnIngressChannel(0, 1, clusteredService, INGRESS_CHANNEL);

        final SimpleEventsImpl sub1 = new SimpleEventsImpl();
        final SimpleEventsImpl sub2 = new SimpleEventsImpl();

        aeronic.registerClusterEgressSubscriber(SimpleEvents.class, sub1, INGRESS_CHANNEL);
        aeronic.registerClusterEgressSubscriber(SimpleEvents.class, sub2, INGRESS_CHANNEL);

        aeronic.awaitUntilPubsAndSubsConnect();
        assertEventuallyTrue(clusteredService::egressConnected);

        clusterEgressSimpleEventsPublisher.onEvent(101L);
        assertEventuallyTrue(() -> sub1.value == 101L && sub2.value == 101L);

        clusterEgressSimpleEventsPublisher.onEvent(201L);
        assertEventuallyTrue(() -> sub1.value == 201L && sub2.value == 201L);
    }

    @Test
    public void udpMulticastEgressPublishing()
    {
        final int streamId = 101;
        final TestClusterNode.Service service = new TestClusterNode.Service();

        clusteredService = new AeronicClusteredServiceContainer(
            new AeronicClusteredServiceContainer.Configuration()
                .clusteredService(service)
                .registerToggledEgressPublisher(SimpleEvents.class, UDP_MULTICAST_CHANNEL, streamId));

        clusterNode = startNodeOnIngressChannel(0, 1, clusteredService, INGRESS_CHANNEL);

        final SimpleEventsImpl sub1 = new SimpleEventsImpl();
        final SimpleEventsImpl sub2 = new SimpleEventsImpl();

        // register as normal (non-cluster) subs
        aeronic.registerSubscriber(SimpleEvents.class, sub1, UDP_MULTICAST_CHANNEL, streamId);
        aeronic.registerSubscriber(SimpleEvents.class, sub2, UDP_MULTICAST_CHANNEL, streamId);

        aeronic.awaitUntilPubsAndSubsConnect();
        assertEventuallyTrue(clusteredService::egressConnected);

        final SimpleEvents udpMulticastPublisher = clusteredService.getToggledPublisherFor(SimpleEvents.class);

        udpMulticastPublisher.onEvent(101L);
        assertEventuallyTrue(() -> sub1.value == 101L && sub2.value == 101L);

        udpMulticastPublisher.onEvent(201L);
        assertEventuallyTrue(() -> sub1.value == 201L && sub2.value == 201L);
    }

    @Test
    public void mdcCastEgressPublishing()
    {
        final int streamId = 101;
        final TestClusterNode.Service service = new TestClusterNode.Service();

        clusteredService = new AeronicClusteredServiceContainer(
            new AeronicClusteredServiceContainer.Configuration()
                .clusteredService(service)
                .registerToggledEgressPublisher(SimpleEvents.class, MDC_CAST_CHANNEL, streamId));

        clusterNode = startNodeOnIngressChannel(0, 1, clusteredService, INGRESS_CHANNEL);

        final SimpleEventsImpl sub1 = new SimpleEventsImpl();
        final SimpleEventsImpl sub2 = new SimpleEventsImpl();

        // register as normal (non-cluster) subs
        aeronic.registerSubscriber(SimpleEvents.class, sub1, MDC_CAST_CHANNEL, streamId);
        aeronic.registerSubscriber(SimpleEvents.class, sub2, MDC_CAST_CHANNEL, streamId);

        aeronic.awaitUntilPubsAndSubsConnect();
        assertEventuallyTrue(clusteredService::egressConnected);

        final SimpleEvents mdcPublisher = clusteredService.getToggledPublisherFor(SimpleEvents.class);

        mdcPublisher.onEvent(101L);
        assertEventuallyTrue(() -> sub1.value == 101L && sub2.value == 101L);

        mdcPublisher.onEvent(201L);
        assertEventuallyTrue(() -> sub1.value == 201L && sub2.value == 201L);
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
