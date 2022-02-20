package io.aeronic.system.multiparam;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeronic.AeronicWizard;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class AeronicMultiParamTest
{

    private static final String IPC = "aeron:ipc";
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
    public void shouldSendAndReceiveOnTopicWithMultipleParams()
    {
        final MultiParamEvents publisher = aeronic.createPublisher(MultiParamEvents.class, IPC, 10);
        final MultiParamEventsImpl subscriberImpl = new MultiParamEventsImpl();
        aeronic.registerSubscriber(MultiParamEvents.class, subscriberImpl, IPC, 10);
        aeronic.awaitUntilPubsAndSubsConnect();

        final long longValue = 2312312341324L;
        final int intValue = 123;
        final float floatValue = 1.21312f;
        final double doubleValue = .03412342;
        final byte byteValue = (byte) 56;
        final char charValue = 'a';
        final boolean booleanValue = true;
        final short shortValue = 123;
        final String stringValue = "stringValue";

        publisher.onEvent(longValue, intValue, floatValue, doubleValue, byteValue, charValue, booleanValue, shortValue, stringValue);

        await()
            .timeout(Duration.ofSeconds(1))
            .until(() -> subscriberImpl.stringValue.equals(stringValue));

        assertEquals(longValue, subscriberImpl.longValue);
        assertEquals(intValue, subscriberImpl.intValue);
        assertEquals(floatValue, subscriberImpl.floatValue);
        assertEquals(doubleValue, subscriberImpl.doubleValue);
        assertEquals(byteValue, subscriberImpl.byteValue);
        assertEquals(charValue, subscriberImpl.charValue);
        assertEquals(booleanValue, subscriberImpl.booleanValue);

        publisher.onEvent(
            123L,
            intValue,
            floatValue,
            doubleValue,
            byteValue,
            charValue,
            false,
            (short) (shortValue + 1),
            stringValue
        );

        await()
            .timeout(Duration.ofSeconds(1))
            .until(() -> subscriberImpl.shortValue == 124);

        assertEquals(123L, subscriberImpl.longValue);
        assertEquals(intValue, subscriberImpl.intValue);
        assertEquals(floatValue, subscriberImpl.floatValue);
        assertEquals(doubleValue, subscriberImpl.doubleValue);
        assertEquals(byteValue, subscriberImpl.byteValue);
        assertEquals(charValue, subscriberImpl.charValue);
        assertFalse(subscriberImpl.booleanValue);
    }

    private static class MultiParamEventsImpl implements MultiParamEvents
    {
        private volatile long longValue;
        private volatile int intValue;
        private volatile float floatValue;
        private volatile double doubleValue;
        private volatile byte byteValue;
        private volatile char charValue;
        private volatile boolean booleanValue;
        private volatile short shortValue;
        private volatile String stringValue;

        @Override
        public void onEvent(
            final long longValue,
            final int intValue,
            final float floatValue,
            final double doubleValue,
            final byte byteValue,
            final char charValue,
            final boolean booleanValue,
            final short shortValue,
            final String stringValue
        )
        {
            this.longValue = longValue;
            this.intValue = intValue;
            this.floatValue = floatValue;
            this.doubleValue = doubleValue;
            this.byteValue = byteValue;
            this.charValue = charValue;
            this.booleanValue = booleanValue;
            this.shortValue = shortValue;
            this.stringValue = stringValue;
        }
    }
}
