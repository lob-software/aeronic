package io.aeronic.codec;

import org.agrona.BitUtil;
import org.agrona.ExpandableDirectByteBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BufferEncoderTest
{
    private ExpandableDirectByteBuffer buffer;
    private BufferEncoder bufferEncoder;

    @BeforeEach
    void setUp()
    {
        buffer = new ExpandableDirectByteBuffer();
        bufferEncoder = new BufferEncoder(buffer);
    }

    @Test
    public void shouldEncodeInt()
    {
        bufferEncoder.encodeInt(123);
        assertEquals(123, buffer.getInt(0));
    }

    @Test
    public void shouldEncodeIntsSuccessively()
    {
        bufferEncoder.encodeInt(123);
        bufferEncoder.encodeInt(456);
        assertEquals(123, buffer.getInt(0));
        assertEquals(456, buffer.getInt(BitUtil.SIZE_OF_INT));
    }

    @Test
    public void shouldEncodeLong()
    {
        final long longValue = 34132123445345L;
        bufferEncoder.encodeLong(longValue);
        assertEquals(longValue, buffer.getLong(0));
    }

    @Test
    public void shouldEncodeFloat()
    {
        final float floatValue = 123123.123123f;
        bufferEncoder.encodeFloat(floatValue);
        assertEquals(floatValue, buffer.getFloat(0));
    }

    @Test
    public void shouldEncodeDouble()
    {
        final double doubleValue = 123123.123123;
        bufferEncoder.encodeDouble(doubleValue);
        assertEquals(doubleValue, buffer.getDouble(0));
    }

    @Test
    public void shouldEncodeByte()
    {
        final byte byteValue = 123;
        bufferEncoder.encodeByte(byteValue);
        assertEquals(byteValue, buffer.getByte(0));
    }

    @Test
    public void shouldEncodeChar()
    {
        final char charValue = 'a';
        bufferEncoder.encodeChar(charValue);
        assertEquals(charValue, buffer.getChar(0));
    }

    @Test
    public void shouldEncodeBoolean()
    {
        final boolean booleanValue = true;
        bufferEncoder.encodeBoolean(booleanValue);
        assertEquals(1, buffer.getByte(0));
    }

    @Test
    public void shouldEncodeShort()
    {
        final short shortValue = 12312;
        bufferEncoder.encodeShort(shortValue);
        assertEquals(shortValue, buffer.getShort(0));
    }

    @Test
    public void shouldEncodeString()
    {
        final String stringValue = "stringValue";
        bufferEncoder.encodeString(stringValue);
        final int encodedLength = buffer.getInt(0);
        final byte[] encodedBytes = new byte[encodedLength];
        buffer.getBytes(BitUtil.SIZE_OF_INT, encodedBytes);

        assertEquals(stringValue.length(), encodedLength);
        assertArrayEquals(stringValue.getBytes(), encodedBytes);
    }
}