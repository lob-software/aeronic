package io.aeronic.net;

import io.aeron.Publication;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public abstract class AbstractPublisher
{
    private final Publication publication;
    private final MutableDirectBuffer buffer;
    protected final BufferEncoder bufferEncoder;

    public AbstractPublisher(final Publication publication)
    {
        this.publication = publication;
        this.buffer = new UnsafeBuffer(ByteBuffer.allocate(64));
        this.bufferEncoder = new BufferEncoder(buffer);
    }

    protected void offer()
    {
        if (publication.isConnected())
        {
            publication.offer(buffer);
        }
        // TODO: rest buffer writer
    }
}
