package io.aeronic;

import io.aeronic.codec.BufferDecoder;
import io.aeronic.codec.BufferEncoder;
import io.aeronic.codec.Decoder;
import io.aeronic.codec.Encoder;

public class SimpleImpl implements Encoder
{
    private final int anInt;
    private final byte aByte;
    private final long aLong;

    public SimpleImpl(final int anInt, final byte aByte, final long aLong)
    {
        this.anInt = anInt;
        this.aByte = aByte;
        this.aLong = aLong;
    }

    @Override
    public void encode(final BufferEncoder bufferEncoder)
    {
        bufferEncoder.encodeInt(anInt);
        bufferEncoder.encodeByte(aByte);
        bufferEncoder.encodeLong(aLong);
    }

    @Decoder
    public static SimpleImpl decode(final BufferDecoder bufferDecoder)
    {
        final int anInt = bufferDecoder.decodeInt();
        final byte aByte = bufferDecoder.decodeByte();
        final long aLong = bufferDecoder.decodeLong();
        return new SimpleImpl(anInt, aByte, aLong);
    }
}
