package io.aeronic.codec;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static io.aeronic.Assertions.assertCodec;

public class ObjectCodecTest {
    @Test
    public void shouldEncodeAndDecodeAnObject()
    {
        assertCodec(new SimpleImpl(1, (byte) 2, 3L, new BigInteger("101010123123123"), new BigDecimal("123412341234.123412341234")));
    }
}
