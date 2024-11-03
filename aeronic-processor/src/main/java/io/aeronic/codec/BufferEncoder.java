package io.aeronic.codec;

import org.agrona.BitUtil;
import org.agrona.MutableDirectBuffer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

public class BufferEncoder
{
    private final MutableDirectBuffer buffer;
    private int currentOffset = 0;

    public BufferEncoder(final MutableDirectBuffer buffer)
    {
        this.buffer = buffer;
    }

    public void encode(final int intValue)
    {
        buffer.putInt(currentOffset, intValue);
        currentOffset += BitUtil.SIZE_OF_INT;
    }

    public void encode(final long longValue)
    {
        buffer.putLong(currentOffset, longValue);
        currentOffset += BitUtil.SIZE_OF_LONG;
    }

    public void encode(final float floatValue)
    {
        buffer.putFloat(currentOffset, floatValue);
        currentOffset += BitUtil.SIZE_OF_FLOAT;
    }

    public void encode(final double doubleValue)
    {
        buffer.putDouble(currentOffset, doubleValue);
        currentOffset += BitUtil.SIZE_OF_DOUBLE;
    }

    public void encode(final byte byteValue)
    {
        buffer.putByte(currentOffset, byteValue);
        currentOffset += BitUtil.SIZE_OF_BYTE;
    }

    public void encode(final char charValue)
    {
        buffer.putChar(currentOffset, charValue);
        currentOffset += BitUtil.SIZE_OF_CHAR;
    }

    public void encode(final boolean booleanValue)
    {
        buffer.putByte(currentOffset, (byte) (booleanValue ? 1 : 0));
        currentOffset += BitUtil.SIZE_OF_BYTE;
    }

    public void encode(final short shortValue)
    {
        buffer.putShort(currentOffset, shortValue);
        currentOffset += BitUtil.SIZE_OF_SHORT;
    }

    public void encode(final String stringValue)
    {
        encode(stringValue.getBytes());
    }

    public void encode(final BigInteger bigInteger)
    {
        encode(bigInteger.toByteArray());
    }

    public void encode(final BigDecimal bigDecimal)
    {
        encode(bigDecimal.toString());
    }

    public void encode(final long[] longs)
    {
        encode(longs.length);
        for (int i = 0; i < longs.length; i++) {
            encode(longs[i]);
        }
    }

    public void encode(final int[] ints)
    {
        encode(ints.length);
        for (int i = 0; i < ints.length; i++) {
            encode(ints[i]);
        }
    }

    public void encode(final float[] floats)
    {
        encode(floats.length);
        for (int i = 0; i < floats.length; i++) {
            encode(floats[i]);
        }
    }

    public void encode(final double[] doubles)
    {
        encode(doubles.length);
        for (int i = 0; i < doubles.length; i++) {
            encode(doubles[i]);
        }
    }

    public void encode(final short[] shorts)
    {
        encode(shorts.length);
        for (int i = 0; i < shorts.length; i++) {
            encode(shorts[i]);
        }
    }

    public void encode(final byte[] bytes)
    {
        encode(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            encode(bytes[i]);
        }
    }

    public void encode(final char[] chars)
    {
        encode(chars.length);
        for (int i = 0; i < chars.length; i++) {
            encode(chars[i]);
        }
    }

    public <T extends Encodable> void encode(final T[] array)
    {
        encode(array.length);
        for (int i = 0; i < array.length; i++) {
            array[i].encode(this);
        }
    }

    public <T extends Encodable> void encode(final List<T> collection)
    {
        final int size = collection.size();
        encode(size);
        collection.forEach(e -> e.encode(this));
    }

    public <T> void encode(final Collection<T> collection, final Encoder<T> encoder)
    {
        final int size = collection.size();
        encode(size);
        collection.forEach(e -> encoder.encode(this, e));
    }

    public void reset()
    {
        currentOffset = 0;
    }
}
