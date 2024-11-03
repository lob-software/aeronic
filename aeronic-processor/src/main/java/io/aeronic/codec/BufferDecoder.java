package io.aeronic.codec;

import org.agrona.BitUtil;
import org.agrona.DirectBuffer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class BufferDecoder
{
    private DirectBuffer buffer;
    private int currentOffset = 0;

    public void wrap(final DirectBuffer buffer, final int offset)
    {
        this.buffer = buffer;
        this.currentOffset = offset;
    }

    public int decodeInt()
    {
        final int intValue = buffer.getInt(currentOffset);
        currentOffset += BitUtil.SIZE_OF_INT;
        return intValue;
    }

    public long decodeLong()
    {
        final long longValue = buffer.getLong(currentOffset);
        currentOffset += BitUtil.SIZE_OF_LONG;
        return longValue;
    }

    public float decodeFloat()
    {
        final float floatValue = buffer.getFloat(currentOffset);
        currentOffset += BitUtil.SIZE_OF_FLOAT;
        return floatValue;
    }

    public double decodeDouble()
    {
        final double doubleValue = buffer.getDouble(currentOffset);
        currentOffset += BitUtil.SIZE_OF_DOUBLE;
        return doubleValue;
    }

    public byte decodeByte()
    {
        final byte byteValue = buffer.getByte(currentOffset);
        currentOffset += BitUtil.SIZE_OF_BYTE;
        return byteValue;
    }

    public char decodeChar()
    {
        final char charValue = buffer.getChar(currentOffset);
        currentOffset += BitUtil.SIZE_OF_CHAR;
        return charValue;
    }

    public boolean decodeBoolean()
    {
        final byte booleanByte = buffer.getByte(currentOffset);
        currentOffset += BitUtil.SIZE_OF_BYTE;
        return booleanByte == 1;
    }

    public short decodeShort()
    {
        final short shortValue = buffer.getShort(currentOffset);
        currentOffset += BitUtil.SIZE_OF_SHORT;
        return shortValue;
    }

    public String decodeString()
    {
        return new String(decodeByteArray());
    }

    public BigInteger decodeBigInteger()
    {
        return new BigInteger(decodeByteArray());
    }

    public BigDecimal decodeBigDecimal()
    {
        return new BigDecimal(decodeString());
    }

    public long[] decodeLongArray()
    {
        final int length = decodeInt();
        final long[] longs = new long[length];
        for (int i = 0; i < longs.length; i++) {
            longs[i] = decodeLong();
        }

        return longs;
    }

    public int[] decodeIntArray()
    {
        final int length = decodeInt();
        final int[] ints = new int[length];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = decodeInt();
        }

        return ints;
    }

    public float[] decodeFloatArray()
    {
        final int length = decodeInt();
        final float[] floats = new float[length];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = decodeFloat();
        }

        return floats;
    }

    public double[] decodeDoubleArray()
    {
        final int length = decodeInt();
        final double[] doubles = new double[length];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = decodeDouble();
        }

        return doubles;
    }

    public short[] decodeShortArray()
    {
        final int length = decodeInt();
        final short[] shorts = new short[length];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = decodeShort();
        }

        return shorts;
    }

    public byte[] decodeByteArray()
    {
        final int length = decodeInt();
        final byte[] bytes = new byte[length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = decodeByte();
        }

        return bytes;
    }

    public char[] decodeCharArray()
    {
        final int length = decodeInt();
        final char[] chars = new char[length];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = decodeChar();
        }

        return chars;
    }

    public <T> T[] decodeArray(final Decoder<T> decoder, final IntFunction<T[]> arrayCreator)
    {
        final int length = decodeInt();
        final T[] array = arrayCreator.apply(length);
        for (int i = 0; i < length; i++) {
            array[i] = decoder.decode(this);
        }
        return array;
    }

    public <T> List<T> decodeList(final Decoder<T> decoder, final Supplier<List<T>> listCreator)
    {
        final int length = decodeInt();
        final List<T> list = listCreator.get();

        for (int i = 0; i < length; i++) {
            list.add(decoder.decode(this));
        }

        return list;
    }
}
