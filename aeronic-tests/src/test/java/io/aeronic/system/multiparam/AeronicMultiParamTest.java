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

import static io.aeronic.Assertions.assertReflectiveEquals;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

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
        aeronic.start();
        aeronic.awaitUntilPubsAndSubsConnect();

        final long longValue = 2312312341324L;
        final int intValue = 123;
        final float floatValue = 1.21312f;
        final double doubleValue = .03412342;
        final byte byteValue = (byte)56;
        final char charValue = 'a';
        final boolean booleanValue = true;
        final short shortValue = 123;
        final String stringValue = "stringValue";
        final Composite compositeValue = new Composite(12, Long.MAX_VALUE, true, Byte.MAX_VALUE, 123.123);
        final long[] longs = { 1L, 2L, 3L, Long.MAX_VALUE, Long.MIN_VALUE };
        final int[] ints = { 1, 2, 3 };
        final double[] doubles = { 1., 2., 3. };
        final float[] floats = { 1.f, 2.f, 3.f };
        final short[] shorts = { 1, 2, 3 };
        final byte[] bytes = { 0x1, 0x2, 0x5 };
        final char[] chars = { '1', '2', '3' };
        final Composite[] compositeArray = new Composite[]{
            new Composite(1, 4L, false, Byte.MAX_VALUE, 123.11),
            new Composite(1, 4L, false, Byte.MIN_VALUE, 123.11)
        };

        publisher.onEvent(
            longValue,
            intValue,
            floatValue,
            doubleValue,
            byteValue,
            charValue,
            booleanValue,
            shortValue,
            stringValue,
            compositeValue,
            longs,
            ints,
            doubles,
            floats,
            shorts,
            bytes,
            chars,
            compositeArray
        );

        await()
            .timeout(Duration.ofSeconds(1))
            .until(() -> {
                // wait for last updated value only because of "happens before"
                assertEquals(shortValue, subscriberImpl.shortValue);
                return true;
            });

        assertEquals(longValue, subscriberImpl.longValue);
        assertEquals(intValue, subscriberImpl.intValue);
        assertEquals(floatValue, subscriberImpl.floatValue);
        assertEquals(doubleValue, subscriberImpl.doubleValue);
        assertEquals(byteValue, subscriberImpl.byteValue);
        assertEquals(charValue, subscriberImpl.charValue);
        assertEquals(booleanValue, subscriberImpl.booleanValue);
        assertEquals(stringValue, subscriberImpl.stringValue);
        assertReflectiveEquals(compositeValue, subscriberImpl.compositeValue);
        assertArrayEquals(longs, subscriberImpl.longs);
        assertArrayEquals(ints, subscriberImpl.ints);
        assertArrayEquals(floats, subscriberImpl.floats);
        assertArrayEquals(doubles, subscriberImpl.doubles);
        assertArrayEquals(shorts, subscriberImpl.shorts);
        assertArrayEquals(bytes, subscriberImpl.bytes);
        assertArrayEquals(chars, subscriberImpl.chars);
        assertReflectiveEquals(compositeArray, subscriberImpl.compositeArray);

        publisher.onEvent(
            123L,
            intValue,
            floatValue,
            doubleValue,
            byteValue,
            charValue,
            false,
            (short)(shortValue + 1),
            stringValue,
            compositeValue,
            longs,
            ints,
            doubles,
            floats,
            shorts,
            bytes,
            chars,
            compositeArray
        );

        await()
            .timeout(Duration.ofSeconds(1))
            .until(() -> {
                // wait for last updated value only because of "happens before"
                assertEquals(shortValue + 1, subscriberImpl.shortValue);
                return true;
            });

        assertEquals(123L, subscriberImpl.longValue);
        assertEquals(intValue, subscriberImpl.intValue);
        assertEquals(floatValue, subscriberImpl.floatValue);
        assertEquals(doubleValue, subscriberImpl.doubleValue);
        assertEquals(byteValue, subscriberImpl.byteValue);
        assertEquals(charValue, subscriberImpl.charValue);
        assertFalse(subscriberImpl.booleanValue);
        assertEquals(stringValue, subscriberImpl.stringValue);
        assertReflectiveEquals(compositeValue, subscriberImpl.compositeValue);
        assertArrayEquals(longs, subscriberImpl.longs);
        assertArrayEquals(ints, subscriberImpl.ints);
        assertArrayEquals(floats, subscriberImpl.floats);
        assertArrayEquals(doubles, subscriberImpl.doubles);
        assertArrayEquals(shorts, subscriberImpl.shorts);
        assertArrayEquals(bytes, subscriberImpl.bytes);
        assertArrayEquals(chars, subscriberImpl.chars);
        assertReflectiveEquals(compositeArray, subscriberImpl.compositeArray);
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
        private volatile Composite compositeValue;
        private volatile long[] longs;
        private volatile int[] ints;
        private volatile double[] doubles;
        private volatile float[] floats;
        private volatile short[] shorts;
        private volatile byte[] bytes;
        private volatile char[] chars;
        private volatile Composite[] compositeArray;

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
            final String stringValue,
            final Composite compositeValue,
            final long[] longs,
            final int[] ints,
            final double[] doubles,
            final float[] floats,
            final short[] shorts,
            final byte[] bytes,
            final char[] chars,
            final Composite[] compositeArray
        )
        {
            this.longValue = longValue;
            this.intValue = intValue;
            this.floatValue = floatValue;
            this.doubleValue = doubleValue;
            this.byteValue = byteValue;
            this.charValue = charValue;
            this.booleanValue = booleanValue;
            this.stringValue = stringValue;
            this.compositeValue = compositeValue;
            this.longs = longs;
            this.ints = ints;
            this.doubles = doubles;
            this.floats = floats;
            this.shorts = shorts;
            this.bytes = bytes;
            this.chars = chars;
            this.compositeArray = compositeArray;
            this.shortValue = shortValue;
        }
    }
}
