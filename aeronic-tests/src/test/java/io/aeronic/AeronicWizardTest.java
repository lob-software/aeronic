package io.aeronic;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeronic.gen.SampleEvents;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

public class AeronicWizardTest
{
    private AeronicWizard aeronic;
    private Aeron aeron;
    private MediaDriver mediaDriver;

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
        final SampleEvents publisher = aeronic.createPublisher(SampleEvents.class);
        final SampleEventsImpl subscriberImpl = new SampleEventsImpl();
        aeronic.registerSubscriber(SampleEvents.class, subscriberImpl);

        publisher.onEvent(123L);

        await()
            .timeout(Duration.ofSeconds(1))
            .until(() -> subscriberImpl.value == 123L);
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

        private long value;

        @Override
        public void onEvent(final long value)
        {
            this.value = value;
        }
    }
}
