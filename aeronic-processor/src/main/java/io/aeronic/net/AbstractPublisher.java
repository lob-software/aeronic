package io.aeronic.net;

import io.aeronic.codec.BufferEncoder;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;

public abstract class AbstractPublisher
{
    private final AeronicPublication publication;
    private final MutableDirectBuffer buffer;
    protected final BufferEncoder bufferEncoder;

    public AbstractPublisher(final AeronicPublication publication)
    {
        this.publication = publication;
        this.buffer = new ExpandableDirectByteBuffer(128);
        this.bufferEncoder = new BufferEncoder(buffer);
    }

    protected void offer()
    {
        if (publication.isConnected())
        {
            publication.offer(buffer);
        }

        bufferEncoder.reset();
    }
}
