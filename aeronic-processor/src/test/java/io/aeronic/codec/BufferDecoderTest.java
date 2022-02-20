package io.aeronic.codec;

import org.agrona.BitUtil;
import org.agrona.ExpandableDirectByteBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BufferDecoderTest
{
    private ExpandableDirectByteBuffer buffer;
    private BufferDecoder bufferDecoder;

    @BeforeEach
    void setUp()
    {
        buffer = new ExpandableDirectByteBuffer();
        bufferDecoder = new BufferDecoder();
    }

    @Test
    public void shouldDecodeInt()
    {
        buffer.putInt(0, 123);
        bufferDecoder.wrap(buffer, 0);

        assertEquals(123, bufferDecoder.decodeInt());
    }

    @Test
    public void shouldDecodeIntsSuccessively()
    {
        buffer.putInt(0, 123);
        buffer.putInt(BitUtil.SIZE_OF_INT, 456);
        bufferDecoder.wrap(buffer, 0);

        assertEquals(123, bufferDecoder.decodeInt());
        assertEquals(456, bufferDecoder.decodeInt());
    }

    @Test
    public void shouldDecodeLong()
    {
        final long longValue = 1231412341234L;
        buffer.putLong(0, longValue);
        bufferDecoder.wrap(buffer, 0);

        assertEquals(longValue, bufferDecoder.decodeLong());
    }

    @Test
    public void shouldDecodeFloat()
    {
        final float floatValue = 1.2312234f;
        buffer.putFloat(0, floatValue);
        bufferDecoder.wrap(buffer, 0);

        assertEquals(floatValue, bufferDecoder.decodeFloat());
    }

    @Test
    public void shouldDecodeDouble()
    {
        final double doubleValue = 1.2323234234;
        buffer.putDouble(0, doubleValue);
        bufferDecoder.wrap(buffer, 0);

        assertEquals(doubleValue, bufferDecoder.decodeDouble());
    }

    @Test
    public void shouldDecodeByte()
    {
        final byte byteValue = 14;
        buffer.putByte(0, byteValue);
        bufferDecoder.wrap(buffer, 0);

        assertEquals(byteValue, bufferDecoder.decodeByte());
    }

    @Test
    public void shouldDecodeChar()
    {
        final char charValue = 'a';
        buffer.putChar(0, charValue);
        bufferDecoder.wrap(buffer, 0);

        assertEquals(charValue, bufferDecoder.decodeChar());
    }

    @Test
    public void shouldDecodeBoolean()
    {
        final byte trueByte = 1;
        buffer.putByte(0, trueByte);
        bufferDecoder.wrap(buffer, 0);

        assertTrue(bufferDecoder.decodeBoolean());
    }

    @Test
    public void shouldDecodeShort()
    {
        final short shortValue = 123;
        buffer.putShort(0, shortValue);
        bufferDecoder.wrap(buffer, 0);

        assertEquals(shortValue, bufferDecoder.decodeShort());
    }

    @Test
    public void shouldDecodeString()
    {
        final String stringValue = "stringValue";
        buffer.putInt(0, stringValue.length());
        buffer.putBytes(BitUtil.SIZE_OF_INT, stringValue.getBytes());
        bufferDecoder.wrap(buffer, 0);

        assertEquals(stringValue, bufferDecoder.decodeString());
    }

    // TODO: all together now
}