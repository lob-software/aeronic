package io.aeronic.net;

import io.aeron.Publication;
import org.agrona.DirectBuffer;

public class MultiplexingAeronicPublication<T> implements AeronicPublication
{
    private final String publisherName;
    private T publisher;
    private final Publication publication;
    private volatile boolean active = false;

    public MultiplexingAeronicPublication(final String publisherName, final T publisher, final Publication publication)
    {
        this.publisherName = publisherName;
        this.publisher = publisher;
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
    }

    @Override
    public void close()
    {
    }

    public void toggleOff()
    {
        active = false;
    }


    public void toggleOn()
    {
        active = true;
    }

    public T getPublisher()
    {
        return publisher;
    }
}
