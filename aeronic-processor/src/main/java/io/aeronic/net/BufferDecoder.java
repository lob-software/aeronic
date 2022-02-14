package io.aeronic.net;

import org.agrona.BitUtil;
import org.agrona.DirectBuffer;

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
        currentOffset += BitUtil.SIZE_OF_BYTE;
        return charValue;
    }
}
