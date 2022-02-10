package io.aeronic;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.NoOpIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

public class AeronicWizardMDCTransportTest
{
    private AeronicWizard aeronic;
    private Aeron aeron;
    private MediaDriver mediaDriver;

    private static final int STREAM_ID = 10;
    private static final String MDC_PUB_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .controlEndpoint("localhost:40456")
        .controlMode("dynamic")
        .endpoint("localhost:40457")
        .build();

    private static final String MDC_SUB_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .controlEndpoint("localhost:40456")
        .controlMode("dynamic")
        .endpoint("localhost:40455")
        .build();

    @BeforeEach
    void setUp()
    {
        final MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
            .dirDeleteOnStart(true)
            .spiesSimulateConnection(true)
            .threadingMode(ThreadingMode.SHARED)
            .sharedIdleStrategy(NoOpIdleStrategy.INSTANCE)
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
        final SampleEvents publisher = aeronic.createPublisher(SampleEvents.class, MDC_PUB_CHANNEL, STREAM_ID);
        final SampleEventsImpl subscriberImpl = new SampleEventsImpl();
        aeronic.registerSubscriber(SampleEvents.class, subscriberImpl, MDC_SUB_CHANNEL, STREAM_ID);

        aeronic.awaitUntilPubsAndSubsConnect();

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

        private volatile long value;

        @Override
        public void onEvent(final long value)
        {
            this.value = value;
        }
    }
}
