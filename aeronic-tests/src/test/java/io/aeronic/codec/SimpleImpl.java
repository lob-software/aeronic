package io.aeronic.codec;

import org.agrona.BitUtil;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

class SimpleImpl
{
    private final int anInt;
    private final byte aByte;
    private final long aLong;

    SimpleImpl(final int anInt, final byte aByte, final long aLong)
    {
        this.anInt = anInt;
        this.aByte = aByte;
        this.aLong = aLong;
    }

    @Encoder
    public void encode(final MutableDirectBuffer buffer)
    {
        buffer.putInt(0, anInt);
        buffer.putByte(BitUtil.SIZE_OF_INT, aByte);
        buffer.putLong(BitUtil.SIZE_OF_INT + BitUtil.SIZE_OF_BYTE, aLong);
    }

    @Decoder
    public static SimpleImpl decode(final DirectBuffer buffer)
    {
        final int anInt = buffer.getInt(0);
        final byte aByte = buffer.getByte(BitUtil.SIZE_OF_INT);
        final long aLong = buffer.getLong(BitUtil.SIZE_OF_INT + BitUtil.SIZE_OF_BYTE);
        return new SimpleImpl(anInt, aByte, aLong);
    }
}
