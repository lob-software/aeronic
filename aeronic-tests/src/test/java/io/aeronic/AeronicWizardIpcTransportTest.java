package io.aeronic;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

public class AeronicWizardIpcTransportTest
{
    private AeronicWizard aeronic;
    private Aeron aeron;
    private MediaDriver mediaDriver;
    private static final String IPC = "aeron:ipc";

    @BeforeEach
    void setUp()
    {
        final MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
            .dirDeleteOnStart(true)
            .threadingMode(ThreadingMode.SHARED)
            .sharedIdleStrategy(new BusySpinIdleStrategy())
            .dirDeleteOnShutdown(true);

        mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);

        final Aeron.Context aeronCtx = new Aeron.Context()
            .aeronDirectoryName(mediaDriver.aeronDirectoryName());

        aeron = Aeron.connect(aeronCtx);
        aeronic = new AeronicWizard(aeron);
    }

    @Test
    public void shouldSendAndReceive()
    {
        final SampleEvents publisher = aeronic.createPublisher(SampleEvents.class, IPC, 10);
        final SampleEventsImpl subscriberImpl = new SampleEventsImpl();
        aeronic.registerSubscriber(SampleEvents.class, subscriberImpl, IPC, 10);

        publisher.onEvent(123L);

        await()
            .timeout(Duration.ofSeconds(1))
            .until(() -> subscriberImpl.value == 123L);
    }

    @Test
    public void shouldSendAndReceiveByMultipleSubscribers()
    {
        final SampleEvents publisher = aeronic.createPublisher(SampleEvents.class, IPC, 10);
        final SampleEventsImpl subscriberImpl1 = new SampleEventsImpl();
        final SampleEventsImpl subscriberImpl2 = new SampleEventsImpl();
        aeronic.registerSubscriber(SampleEvents.class, subscriberImpl1, IPC, 10);
        aeronic.registerSubscriber(SampleEvents.class, subscriberImpl2, IPC, 10);

        publisher.onEvent(123L);

        await()
            .timeout(Duration.ofSeconds(1))
            .until(() -> subscriberImpl1.value == 123L && subscriberImpl2.value == 123L);
    }

    @Test
    public void shouldSendAndReceiveByMultipleSubscribersOfMoreThanOneTopic()
    {
        final SampleEvents sampleEventsPublisher = aeronic.createPublisher(SampleEvents.class, IPC, 10);

        final SampleEventsImpl sampleEventsSubscriber1 = new SampleEventsImpl();
        final SampleEventsImpl sampleEventsSubscriber2 = new SampleEventsImpl();
        aeronic.registerSubscriber(SampleEvents.class, sampleEventsSubscriber1, IPC, 10);
        aeronic.registerSubscriber(SampleEvents.class, sampleEventsSubscriber2, IPC, 10);

        sampleEventsPublisher.onEvent(123L);

        final SimpleEvents simpleEventsPublisher = aeronic.createPublisher(SimpleEvents.class, IPC, 11);

        final SimpleEventsImpl simpleEventsSubscriber1 = new SimpleEventsImpl();
        final SimpleEventsImpl simpleEventsSubscriber2 = new SimpleEventsImpl();
        aeronic.registerSubscriber(SimpleEvents.class, simpleEventsSubscriber1, IPC, 11);
        aeronic.registerSubscriber(SimpleEvents.class, simpleEventsSubscriber2, IPC, 11);

        simpleEventsPublisher.onEvent(456L);

        await()
            .timeout(Duration.ofSeconds(1))
            .until(() -> sampleEventsSubscriber1.value == 123L && sampleEventsSubscriber2.value == 123L);

        await()
            .timeout(Duration.ofSeconds(1))
            .until(() -> simpleEventsSubscriber1.value == 456L && simpleEventsSubscriber2.value == 456L);
    }

    @AfterEach
    void tearDown()
    {
        aeronic.close();
        aeron.close();
        mediaDriver.close();
    }

    private static class SampleEventsImpl implements SampleEvents
    {

        private volatile long value;

        @Override
        public void onEvent(final long value)
        {
            this.value = value;
        }
    }

    private static class SimpleEventsImpl implements SimpleEvents
    {

        private volatile long value;

        @Override
        public void onEvent(final long value)
        {
            this.value = value;
        }
    }
}
