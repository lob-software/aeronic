package io.aeronic.net;

import org.junit.jupiter.api.Test;

import static io.aeronic.Assertions.assertCodec;

public class CodecTest
{
    @Test
    public void shouldEncodeAndDecodeAnObject()
    {
        assertCodec(new SimpleImpl(1, (byte)2, 3L));
    }
}
