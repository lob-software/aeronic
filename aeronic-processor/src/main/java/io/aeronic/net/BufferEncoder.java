package io.aeronic.net;

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
}
