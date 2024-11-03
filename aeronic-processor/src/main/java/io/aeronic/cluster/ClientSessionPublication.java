package io.aeronic.cluster;

import io.aeron.cluster.service.ClientSession;
import io.aeronic.net.AeronicPublication;
import org.agrona.DirectBuffer;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class ClientSessionPublication<T> implements AeronicPublication {
    private final String publisherName;
    private final Set<ClientSession> clientSessions = Collections.newSetFromMap(new IdentityHashMap<>());
    private T publisher;

    public ClientSessionPublication(final String publisherName)
    {
        this.publisherName = publisherName;
    }

    @Override
    public boolean isConnected()
    {
        return !clientSessions.isEmpty() && clientSessions.stream().noneMatch(ClientSession::isClosing);
    }

    @Override
    public long offer(final DirectBuffer buffer)
    {
        clientSessions.forEach(s -> s.offer(buffer, 0, buffer.capacity()));
        // FIXME: how should offer results be aggregated?
        return 0;
    }

    @Override
    public void close()
    {
        if (isConnected()) {
            clientSessions.forEach(ClientSession::close);
        }
    }

    public void bindClientSession(final ClientSession clientSession)
    {
        clientSessions.add(clientSession);
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
