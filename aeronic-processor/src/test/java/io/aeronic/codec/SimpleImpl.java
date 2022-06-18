package io.aeronic.codec;

import java.math.BigDecimal;
import java.math.BigInteger;

public class SimpleImpl implements Encodable
{
    private final int anInt;
    private final byte aByte;
    private final long aLong;
    private final BigInteger bigInteger;
    private final BigDecimal bigDecimal;

    public SimpleImpl(final int anInt, final byte aByte, final long aLong, final BigInteger bigInteger, final BigDecimal bigDecimal)
    {
        this.anInt = anInt;
        this.aByte = aByte;
        this.aLong = aLong;
        this.bigInteger = bigInteger;
        this.bigDecimal = bigDecimal;
    }

    @Override
    public void encode(final BufferEncoder bufferEncoder)
    {
        bufferEncoder.encode(anInt);
        bufferEncoder.encode(aByte);
        bufferEncoder.encode(aLong);
        bufferEncoder.encode(bigInteger);
        bufferEncoder.encode(bigDecimal);
    }

    @DecodedBy
    public static SimpleImpl decode(final BufferDecoder bufferDecoder)
    {
        final int anInt = bufferDecoder.decodeInt();
        final byte aByte = bufferDecoder.decodeByte();
        final long aLong = bufferDecoder.decodeLong();
        final BigInteger bigInteger = bufferDecoder.decodeBigInteger();
        final BigDecimal bigDecimal = bufferDecoder.decodeBigDecimal();
        return new SimpleImpl(anInt, aByte, aLong, bigInteger, bigDecimal);
    }
}
