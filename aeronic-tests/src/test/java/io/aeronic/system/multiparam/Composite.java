package io.aeronic.system.multiparam;

import io.aeronic.codec.BufferDecoder;
import io.aeronic.codec.BufferEncoder;
import io.aeronic.codec.DecodedBy;
import io.aeronic.codec.Encodable;

public class Composite implements Encodable
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
        bufferEncoder.encode(anInt);
        bufferEncoder.encode(aLong);
        bufferEncoder.encode(aBoolean);
        bufferEncoder.encode(aByte);
        bufferEncoder.encode(aDouble);
    }

    @DecodedBy
    public static Composite decode(final BufferDecoder bufferDecoder)
    {
        final int anInt = bufferDecoder.decodeInt();
        final long aLong = bufferDecoder.decodeLong();
        final boolean aBoolean = bufferDecoder.decodeBoolean();
        final byte aByte = bufferDecoder.decodeByte();
        final double aDouble = bufferDecoder.decodeDouble();
        return new Composite(anInt, aLong, aBoolean, aByte, aDouble);
    }

    @Override
    public String toString()
    {
        return "Composite{" +
            "anInt=" + anInt +
            ", aLong=" + aLong +
            ", aBoolean=" + aBoolean +
            ", aByte=" + aByte +
            ", aDouble=" + aDouble +
            '}';
    }
}
