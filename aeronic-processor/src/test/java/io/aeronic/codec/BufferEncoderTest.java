package io.aeronic.codec;

import org.agrona.BitUtil;
import org.agrona.ExpandableDirectByteBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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
        bufferEncoder.encode(123);
        assertEquals(123, buffer.getInt(0));
    }

    @Test
    public void shouldEncodeIntsSuccessively()
    {
        bufferEncoder.encode(123);
        bufferEncoder.encode(456);
        assertEquals(123, buffer.getInt(0));
        assertEquals(456, buffer.getInt(BitUtil.SIZE_OF_INT));
    }

    @Test
    public void shouldEncodeLong()
    {
        final long longValue = 34132123445345L;
        bufferEncoder.encode(longValue);
        assertEquals(longValue, buffer.getLong(0));
    }

    @Test
    public void shouldEncodeFloat()
    {
        final float floatValue = 123123.123123f;
        bufferEncoder.encode(floatValue);
        assertEquals(floatValue, buffer.getFloat(0));
    }

    @Test
    public void shouldEncodeDouble()
    {
        final double doubleValue = 123123.123123;
        bufferEncoder.encode(doubleValue);
        assertEquals(doubleValue, buffer.getDouble(0));
    }

    @Test
    public void shouldEncodeByte()
    {
        final byte byteValue = 123;
        bufferEncoder.encode(byteValue);
        assertEquals(byteValue, buffer.getByte(0));
    }

    @Test
    public void shouldEncodeChar()
    {
        final char charValue = 'a';
        bufferEncoder.encode(charValue);
        assertEquals(charValue, buffer.getChar(0));
    }

    @Test
    public void shouldEncodeBoolean()
    {
        final boolean booleanValue = true;
        bufferEncoder.encode(booleanValue);
        assertEquals(1, buffer.getByte(0));
    }

    @Test
    public void shouldEncodeShort()
    {
        final short shortValue = 12312;
        bufferEncoder.encode(shortValue);
        assertEquals(shortValue, buffer.getShort(0));
    }

    @Test
    public void shouldEncodeString()
    {
        final String stringValue = "stringValue";
        bufferEncoder.encode(stringValue);
        final int encodedLength = buffer.getInt(0);
        final byte[] encodedBytes = new byte[encodedLength];
        buffer.getBytes(BitUtil.SIZE_OF_INT, encodedBytes);

        assertEquals(stringValue.length(), encodedLength);
        assertArrayEquals(stringValue.getBytes(), encodedBytes);
    }

    @Test
    public void shouldEncodeLongArray()
    {
        final long[] longs = {1L, 2L, 4L, 5L};
        bufferEncoder.encode(longs);
        final int encodedLength = buffer.getInt(0);
        final long[] encodedArray = new long[encodedLength];

        int idx = BitUtil.SIZE_OF_INT;
        for (int i = 0; i < encodedLength; i++)
        {
            encodedArray[i] = buffer.getLong(idx);
            idx += BitUtil.SIZE_OF_LONG;
        }

        assertArrayEquals(longs, encodedArray);
    }

    @Test
    public void shouldEncodeIntArray()
    {
        final int[] ints = {1, 2, 4, 5};
        bufferEncoder.encode(ints);
        final int encodedLength = buffer.getInt(0);
        final int[] encodedArray = new int[encodedLength];

        int idx = BitUtil.SIZE_OF_INT;
        for (int i = 0; i < encodedLength; i++)
        {
            encodedArray[i] = buffer.getInt(idx);
            idx += BitUtil.SIZE_OF_INT;
        }

        assertArrayEquals(ints, encodedArray);
    }

    @Test
    public void shouldEncodeFloatArray()
    {
        final float[] floats = {1.f, 2.f, 4.f, 5.f};
        bufferEncoder.encode(floats);
        final int encodedLength = buffer.getInt(0);
        final float[] encodedArray = new float[encodedLength];

        int idx = BitUtil.SIZE_OF_INT;
        for (int i = 0; i < encodedLength; i++)
        {
            encodedArray[i] = buffer.getFloat(idx);
            idx += BitUtil.SIZE_OF_FLOAT;
        }

        assertArrayEquals(floats, encodedArray);
    }

    @Test
    public void shouldEncodeDoubleArray()
    {
        final double[] doubles = {1., 2., 4., 5.};
        bufferEncoder.encode(doubles);
        final int encodedLength = buffer.getInt(0);
        final double[] encodedArray = new double[encodedLength];

        int idx = BitUtil.SIZE_OF_INT;
        for (int i = 0; i < encodedLength; i++)
        {
            encodedArray[i] = buffer.getDouble(idx);
            idx += BitUtil.SIZE_OF_DOUBLE;
        }

        assertArrayEquals(doubles, encodedArray);
    }

    @Test
    public void shouldEncodeShortArray()
    {
        final short[] shorts = {1, 2, 4, 5};
        bufferEncoder.encode(shorts);
        final int encodedLength = buffer.getInt(0);
        final short[] encodedArray = new short[encodedLength];

        int idx = BitUtil.SIZE_OF_INT;
        for (int i = 0; i < encodedLength; i++)
        {
            encodedArray[i] = buffer.getShort(idx);
            idx += BitUtil.SIZE_OF_SHORT;
        }

        assertArrayEquals(shorts, encodedArray);
    }

    @Test
    public void shouldEncodeByteArray()
    {
        final byte[] bytes = {0x1, 0x2, 0x3, 0x5};
        bufferEncoder.encode(bytes);
        final int encodedLength = buffer.getInt(0);
        final byte[] encodedArray = new byte[encodedLength];

        int idx = BitUtil.SIZE_OF_INT;
        buffer.getBytes(idx, encodedArray);

        assertArrayEquals(bytes, encodedArray);
    }

    @Test
    public void shouldEncodeCharArray()
    {
        final char[] chars = {'1', '2', '3', '5'};
        bufferEncoder.encode(chars);
        final int encodedLength = buffer.getInt(0);
        final char[] encodedArray = new char[encodedLength];

        int idx = BitUtil.SIZE_OF_INT;
        for (int i = 0; i < encodedLength; i++)
        {
            encodedArray[i] = buffer.getChar(idx);
            idx += BitUtil.SIZE_OF_CHAR;
        }

        assertArrayEquals(chars, encodedArray);
    }

    @Test
    public void shouldEncodeBigInteger()
    {
        bufferEncoder.encode(BigInteger.TEN);
        final int arrLen = buffer.getInt(0);
        final byte[] bigIntBytes = new byte[arrLen];
        buffer.getBytes(BitUtil.SIZE_OF_INT, bigIntBytes);
        final BigInteger actual = new BigInteger(bigIntBytes);
        assertEquals(BigInteger.TEN, actual);
    }

    @Test
    public void shouldEncodeBigDecimal()
    {
        bufferEncoder.encode(BigDecimal.TEN);
        final int arrLen = buffer.getInt(0);
        final byte[] bigDecimalBytes = new byte[arrLen];
        buffer.getBytes(BitUtil.SIZE_OF_INT, bigDecimalBytes);
        final BigDecimal actual = new BigDecimal(new String(bigDecimalBytes));
        assertEquals(BigDecimal.TEN, actual);
    }

    @Test
    public void shouldEncodeList()
    {
        final List<Long> longList = List.of(1L, 2L, 3L, Long.MAX_VALUE, Long.MIN_VALUE);
        bufferEncoder.encode(longList, BufferEncoder::encode);

        final int encodedLength = buffer.getInt(0);
        final List<Long> encodedCollection = new ArrayList<>();

        int idx = BitUtil.SIZE_OF_INT;
        for (int i = 0; i < encodedLength; i++)
        {
            encodedCollection.add(buffer.getLong(idx));
            idx += BitUtil.SIZE_OF_LONG;
        }

        assertEquals(longList, encodedCollection);
    }
}