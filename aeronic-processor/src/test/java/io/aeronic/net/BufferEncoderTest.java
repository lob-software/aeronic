package io.aeronic.net;

import org.agrona.BitUtil;
import org.agrona.ExpandableDirectByteBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BufferEncoderTest
{
    private ExpandableDirectByteBuffer buffer;
    private BufferEncoder bufferEncoder;

    @BeforeEach
    void setUp()
    {
        buffer = new ExpandableDirectByteBuffer();
        bufferEncoder = new BufferEncoder(buffer);
    }

    @Test
    public void shouldEncodeInt()
    {
        bufferEncoder.encodeInt(123);
        assertEquals(123, buffer.getInt(0));
    }

    @Test
    public void shouldEncodeIntsSuccessively()
    {
        bufferEncoder.encodeInt(123);
        bufferEncoder.encodeInt(456);
        assertEquals(123, buffer.getInt(0));
        assertEquals(456, buffer.getInt(BitUtil.SIZE_OF_INT));
    }

    @Test
    public void shouldEncodeLong()
    {
        final long longValue = 34132123445345L;
        bufferEncoder.encodeLong(longValue);
        assertEquals(longValue, buffer.getLong(0));
    }
}