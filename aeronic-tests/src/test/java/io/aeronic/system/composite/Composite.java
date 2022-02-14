package io.aeronic.system.composite;

import io.aeronic.codec.BufferDecoder;
import io.aeronic.codec.BufferEncoder;
import io.aeronic.codec.Decoder;
import io.aeronic.codec.Encoder;

public class Composite implements Encoder
{
    private final int anInt;
    private final long aLong;
    private final boolean aBoolean;
    private final byte aByte;
    private final double aDouble;

    public Composite(final int anInt, final long aLong, final boolean aBoolean, final byte aByte, final double aDouble)
    {
        this.anInt = anInt;
        this.aLong = aLong;
        this.aBoolean = aBoolean;
        this.aByte = aByte;
        this.aDouble = aDouble;
    }

    @Override
    public void encode(final BufferEncoder bufferEncoder)
    {
        bufferEncoder.encodeInt(anInt);
        bufferEncoder.encodeLong(aLong);
        bufferEncoder.encodeBoolean(aBoolean);
        bufferEncoder.encodeByte(aByte);
        bufferEncoder.encodeDouble(aDouble);
    }

    @Decoder
    public static Composite decode(final BufferDecoder bufferDecoder)
    {
        final int anInt = bufferDecoder.decodeInt();
        final long aLong = bufferDecoder.decodeLong();
        final boolean aBoolean = bufferDecoder.decodeBoolean();
        final byte aByte = bufferDecoder.decodeByte();
        final double aDouble = bufferDecoder.decodeDouble();
        return new Composite(anInt, aLong, aBoolean, aByte, aDouble);
    }
}
