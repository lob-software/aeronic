package io.aeronic.cluster;

import io.aeron.cluster.service.ClientSession;
import io.aeronic.net.AeronicPublication;
import org.agrona.DirectBuffer;

public class ClientSessionPublication<T> implements AeronicPublication
{
    private final String publisherName;
    private ClientSession clientSession;
    private T publisher;

    public ClientSessionPublication(final String publisherName)
    {
        this.publisherName = publisherName;
    }

    @Override
    public boolean isConnected()
    {
        return clientSession != null && !clientSession.isClosing();
    }

    @Override
    public void offer(final DirectBuffer buffer)
    {
        clientSession.offer(buffer, 0, buffer.capacity());
    }

    public void bindClientSession(final ClientSession clientSession)
    {
        this.clientSession = clientSession;
    }

    public void bindPublisher(final T publisher)
    {
        this.publisher = publisher;
    }

    public String getName()
    {
        return publisherName;
    }

    public T getPublisher()
    {
        return publisher;
    }
}
