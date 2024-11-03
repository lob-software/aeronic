package io.aeronic.codec;

import org.agrona.BitUtil;
import org.agrona.ExpandableDirectByteBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    public void shouldDecodeLongArray()
    {
        final long[] longs = { 1L, 2L, 4L, 5L };
        buffer.putInt(0, longs.length);

        int idx = BitUtil.SIZE_OF_INT;
        for (int i = 0; i < longs.length; i++)
        {
            buffer.putLong(idx, longs[i]);
            idx += BitUtil.SIZE_OF_LONG;
        }

        bufferDecoder.wrap(buffer, 0);
        assertArrayEquals(longs, bufferDecoder.decodeLongArray());
    }

    @Test
    public void shouldDecodeIntArray()
    {
        final int[] ints = { 1, 2, 4, 5 };
        buffer.putInt(0, ints.length);

        int idx = BitUtil.SIZE_OF_INT;
        for (int i = 0; i < ints.length; i++)
        {
            buffer.putInt(idx, ints[i]);
            idx += BitUtil.SIZE_OF_INT;
        }

        bufferDecoder.wrap(buffer, 0);
        assertArrayEquals(ints, bufferDecoder.decodeIntArray());
    }

    @Test
    public void shouldDecodeFloatArray()
    {
        final float[] floats = { 1.f, 2.f, 4.f, 5.f };
        buffer.putInt(0, floats.length);

        int idx = BitUtil.SIZE_OF_INT;
        for (int i = 0; i < floats.length; i++)
        {
            buffer.putFloat(idx, floats[i]);
            idx += BitUtil.SIZE_OF_FLOAT;
        }

        bufferDecoder.wrap(buffer, 0);
        assertArrayEquals(floats, bufferDecoder.decodeFloatArray());
    }

    @Test
    public void shouldDecodeDoubleArray()
    {
        final double[] doubles = { 1., 2., 4., 5. };
        buffer.putInt(0, doubles.length);

        int idx = BitUtil.SIZE_OF_INT;
        for (int i = 0; i < doubles.length; i++)
        {
            buffer.putDouble(idx, doubles[i]);
            idx += BitUtil.SIZE_OF_DOUBLE;
        }

        bufferDecoder.wrap(buffer, 0);
        assertArrayEquals(doubles, bufferDecoder.decodeDoubleArray());
    }

    @Test
    public void shouldDecodeShortArray()
    {
        final short[] shorts = { 1, 2, 4, 5 };
        buffer.putInt(0, shorts.length);

        int idx = BitUtil.SIZE_OF_INT;
        for (int i = 0; i < shorts.length; i++)
        {
            buffer.putShort(idx, shorts[i]);
            idx += BitUtil.SIZE_OF_SHORT;
        }

        bufferDecoder.wrap(buffer, 0);
        assertArrayEquals(shorts, bufferDecoder.decodeShortArray());
    }

    @Test
    public void shouldDecodeByteArray()
    {
        final byte[] bytes = { 0x1, 0x2, 0x3, 0x5 };
        buffer.putInt(0, bytes.length);
        buffer.putBytes(BitUtil.SIZE_OF_INT, bytes);

        bufferDecoder.wrap(buffer, 0);
        assertArrayEquals(bytes, bufferDecoder.decodeByteArray());
    }

    @Test
    public void shouldDecodeCharArray()
    {
        final char[] chars = { '1', '2', '4', '5' };
        buffer.putInt(0, chars.length);

        int idx = BitUtil.SIZE_OF_INT;
        for (int i = 0; i < chars.length; i++)
        {
            buffer.putChar(idx, chars[i]);
            idx += BitUtil.SIZE_OF_CHAR;
        }

        bufferDecoder.wrap(buffer, 0);
        assertArrayEquals(chars, bufferDecoder.decodeCharArray());
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

    @Test
    public void shouldDecodeBigInteger()
    {
        final BigInteger bigInteger = BigInteger.TEN;
        buffer.putInt(0, bigInteger.toByteArray().length);
        buffer.putBytes(BitUtil.SIZE_OF_INT, bigInteger.toByteArray());
        bufferDecoder.wrap(buffer, 0);

        assertEquals(bigInteger, bufferDecoder.decodeBigInteger());
    }

    @Test
    public void shouldDecodeBigDecimal()
    {
        final BigDecimal bigDecimal = BigDecimal.ONE;
        buffer.putInt(0, bigDecimal.toString()
            .length());
        buffer.putBytes(BitUtil.SIZE_OF_INT, bigDecimal.toString()
            .getBytes());
        bufferDecoder.wrap(buffer, 0);

        assertEquals(bigDecimal, bufferDecoder.decodeBigDecimal());
    }

    @Test
    public void shouldDecodeCollection()
    {
        final List<Long> list = List.of(1L, 2L, Long.MAX_VALUE, Long.MIN_VALUE);
        buffer.putInt(0, list.size());

        int idx = BitUtil.SIZE_OF_INT;
        for (int i = 0; i < list.size(); i++)
        {
            buffer.putLong(idx, list.get(i));
            idx += BitUtil.SIZE_OF_LONG;
        }

        bufferDecoder.wrap(buffer, 0);
        final Collection<Long> decodeCollection = bufferDecoder.decodeList(BufferDecoder::decodeLong, ArrayList::new);
        assertEquals(list, decodeCollection);
    }
}