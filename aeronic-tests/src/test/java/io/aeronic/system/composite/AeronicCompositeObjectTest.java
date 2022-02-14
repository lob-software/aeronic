package io.aeronic.system.composite;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeronic.AeronicWizard;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.aeronic.Assertions.assertReflectiveEquals;
import static org.awaitility.Awaitility.await;

public class AeronicCompositeObjectTest
{
    public static final String IPC = "aeron:ipc";
    private AeronicWizard aeronic;
    private Aeron aeron;
    private MediaDriver mediaDriver;

    @BeforeEach
    void setUp()
    {
        final MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
            .dirDeleteOnStart(true)
            .spiesSimulateConnection(true)
            .threadingMode(ThreadingMode.SHARED)
            .sharedIdleStrategy(new BusySpinIdleStrategy())
            .dirDeleteOnShutdown(true);

        mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);

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
    }

    @Test
    public void shouldSendAndReceiveOnTopicWithCompositeParam()
    {
        final CompositeObjectEvents publisher = aeronic.createPublisher(CompositeObjectEvents.class, IPC, 10);
        final CompositeObjectEventsImpl subscriberImpl = new CompositeObjectEventsImpl();
        aeronic.registerSubscriber(CompositeObjectEvents.class, subscriberImpl, IPC, 10);
        aeronic.awaitUntilPubsAndSubsConnect();

        final Composite expected = new Composite(12, Long.MAX_VALUE, true, Byte.MAX_VALUE, 123.123);
        publisher.onEvent(expected);

        await()
            .timeout(Duration.ofSeconds(1))
            .until(() -> {
                assertReflectiveEquals(expected, subscriberImpl.composite);
                return true;
            });
    }

    private static class CompositeObjectEventsImpl implements CompositeObjectEvents
    {
        private volatile Composite composite;

        @Override
        public void onEvent(final Composite composite)
        {
            this.composite = composite;
        }
    }
}
