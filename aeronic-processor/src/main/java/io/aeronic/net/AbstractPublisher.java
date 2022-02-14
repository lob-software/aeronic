package io.aeronic.net;

import io.aeron.Publication;
import io.aeronic.codec.BufferEncoder;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;

public abstract class AbstractPublisher
{
    private final Publication publication;
    private final MutableDirectBuffer buffer;
    protected final BufferEncoder bufferEncoder;

    public AbstractPublisher(final Publication publication)
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
