package io.aeronic.codec;

import org.agrona.BitUtil;
import org.agrona.MutableDirectBuffer;

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

    public void encode(final long[] longArray)
    {
        encode(longArray.length);
        for (int i = 0; i < longArray.length; i++)
        {
            encode(longArray[i]);
        }
    }

    public void encode(final int[] intArray)
    {
        encode(intArray.length);
        for (int i = 0; i < intArray.length; i++)
        {
            encode(intArray[i]);
        }
    }

    public void encode(final float[] floatArray)
    {
        encode(floatArray.length);
        for (int i = 0; i < floatArray.length; i++)
        {
            encode(floatArray[i]);
        }
    }

    public void encode(final double[] doubleArray)
    {
        encode(doubleArray.length);
        for (int i = 0; i < doubleArray.length; i++)
        {
            encode(doubleArray[i]);
        }
    }

    public void encode(final byte[] byteArray)
    {
        encode(byteArray.length);
        for (int i = 0; i < byteArray.length; i++)
        {
            encode(byteArray[i]);
        }
    }

    public void encode(final char[] charArray)
    {
        encode(charArray.length);
        for (int i = 0; i < charArray.length; i++)
        {
            encode(charArray[i]);
        }
    }

    public void reset()
    {
        currentOffset = 0;
    }
}
