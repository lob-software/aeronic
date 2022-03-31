package io.aeronic.net;

import io.aeron.Publication;
import org.agrona.DirectBuffer;

public class SimplePublication implements AeronicPublication
{

    private final Publication publication;

    public SimplePublication(final Publication publication)
    {
        this.publication = publication;
    }

    @Override
    public boolean isConnected()
    {
        return publication.isConnected();
    }

    @Override
    public void offer(final DirectBuffer buffer)
    {
        publication.offer(buffer);
    }
}
