package io.aeronic.system.cluster;

import io.aeron.Aeron;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeronic.AeronicWizard;
import io.aeronic.SampleEvents;
import io.aeronic.SimpleEvents;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

public class ClusterSystemTest
{
    private AeronicWizard aeronic;
    private Aeron aeron;
    private MediaDriver mediaDriver;
    private TestClusterNode clusterNode;
    private SimpleEventsImpl simpleEvents;
    private SampleEventsImpl sampleEvents;

    @BeforeEach
    void setUp()
    {
        final MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
            .dirDeleteOnStart(true)
            .dirDeleteOnShutdown(true)
            .spiesSimulateConnection(true)
            .threadingMode(ThreadingMode.SHARED)
            .sharedIdleStrategy(new BusySpinIdleStrategy());

        mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);

        final Aeron.Context aeronCtx = new Aeron.Context()
            .aeronDirectoryName(mediaDriver.aeronDirectoryName());

        aeron = Aeron.connect(aeronCtx);
        aeronic = new AeronicWizard(aeron);
        simpleEvents = new SimpleEventsImpl();
        sampleEvents = new SampleEventsImpl();
        clusterNode = new TestClusterNode(new TestClusterNode.Service(simpleEvents, sampleEvents), true);
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
    public void cluster()
    {
        final AeronCluster simpleEventsClusterClient = clusterNode.connectClientToCluster(SimpleEvents.class.getName());
        final AeronCluster sampleEventsClusterClient = clusterNode.connectClientToCluster(SampleEvents.class.getName());

        final SimpleEvents simpleEventsPublisher = aeronic.createClusterPublisher(SimpleEvents.class, simpleEventsClusterClient);
        final SampleEvents sampleEventsPublisher = aeronic.createClusterPublisher(SampleEvents.class, sampleEventsClusterClient);

        simpleEventsPublisher.onEvent(101L);
        sampleEventsPublisher.onEvent(201L);

        await()
            .timeout(Duration.ofSeconds(1))
            .until(() -> simpleEvents.value == 101L && sampleEvents.value == 201L);

        simpleEventsClusterClient.close();
        sampleEventsClusterClient.close();
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
