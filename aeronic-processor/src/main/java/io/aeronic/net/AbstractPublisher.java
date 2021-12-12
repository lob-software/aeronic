package io.aeronic.net;

import io.aeron.Publication;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

public abstract class AbstractPublisher
{
    private final Publication publication;
    protected final UnsafeBuffer unsafeBuffer;

    public AbstractPublisher(final Publication publication)
    {
        this.publication = publication;
        this.unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(64));
    }

    protected void offer()
    {
        if (publication.isConnected())
        {
            publication.offer(unsafeBuffer);
        }
    }
}
