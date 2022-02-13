package io.aeronic.net;

import org.agrona.BitUtil;
import org.agrona.ExpandableDirectByteBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BufferDecoderTest
{
    private ExpandableDirectByteBuffer buffer;
    private BufferDecoder bufferDecoder;

    @BeforeEach
    void setUp()
    {
        buffer = new ExpandableDirectByteBuffer();
        bufferDecoder = new BufferDecoder();
    }

    @Test
    public void shouldDecodeInt()
    {
        buffer.putInt(0, 123);
        bufferDecoder.wrap(buffer, 0);

        assertEquals(123, bufferDecoder.decodeInt());
    }

    @Test
    public void shouldDecodeIntsSuccessively()
    {
        buffer.putInt(0, 123);
        buffer.putInt(BitUtil.SIZE_OF_INT, 456);
        bufferDecoder.wrap(buffer, 0);

        assertEquals(123, bufferDecoder.decodeInt());
        assertEquals(456, bufferDecoder.decodeInt());
    }

    @Test
    public void shouldDecodeLong()
    {
        final long longValue = 1231412341234L;
        buffer.putLong(0, longValue);
        bufferDecoder.wrap(buffer, 0);

        assertEquals(longValue, bufferDecoder.decodeLong());
    }
}