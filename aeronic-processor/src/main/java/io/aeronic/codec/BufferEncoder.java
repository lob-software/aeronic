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

    public void encodeInt(final int intValue)
    {
        buffer.putInt(currentOffset, intValue);
        currentOffset += BitUtil.SIZE_OF_INT;
    }

    public void encodeLong(final long longValue)
    {
        buffer.putLong(currentOffset, longValue);
        currentOffset += BitUtil.SIZE_OF_LONG;
    }

    public void encodeFloat(final float floatValue)
    {
        buffer.putFloat(currentOffset, floatValue);
        currentOffset += BitUtil.SIZE_OF_FLOAT;
    }

    public void encodeDouble(final double doubleValue)
    {
        buffer.putDouble(currentOffset, doubleValue);
        currentOffset += BitUtil.SIZE_OF_DOUBLE;
    }

    public void encodeByte(final byte byteValue)
    {
        buffer.putByte(currentOffset, byteValue);
        currentOffset += BitUtil.SIZE_OF_BYTE;
    }

    public void encodeChar(final char charValue)
    {
        buffer.putChar(currentOffset, charValue);
        currentOffset += BitUtil.SIZE_OF_CHAR;
    }

    public void encodeBoolean(final boolean booleanValue)
    {
        buffer.putByte(currentOffset, (byte) (booleanValue ? 1 : 0));
        currentOffset += BitUtil.SIZE_OF_BYTE;
    }

    public void encodeShort(final short shortValue)
    {
        buffer.putShort(currentOffset, shortValue);
        currentOffset += BitUtil.SIZE_OF_SHORT;
    }

    public void encodeString(final String stringValue)
    {
        encodeInt(stringValue.length());
        for (final byte b : stringValue.getBytes())
        {
            encodeByte(b);
        }
    }

    public void reset()
    {
        currentOffset = 0;
    }
}
