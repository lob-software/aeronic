package io.aeronic.net;

import io.aeron.Publication;
import org.agrona.DirectBuffer;

import java.util.function.Supplier;

public class MultiplexingAeronicPublication<T> implements AeronicPublication
{
    private final Supplier<Publication> publicationSupplier;
    private Publication publication;
    private boolean active = false;
    private T publisher;

    public MultiplexingAeronicPublication(final Supplier<Publication> publicationSupplier)
    {
        this.publicationSupplier = publicationSupplier;
    }

    @Override
    public boolean isConnected()
    {
        return active && publication.isConnected();
    }

    @Override
    public void offer(final DirectBuffer buffer)
    {
        publication.offer(buffer);
    }

    @Override
    public void close()
    {
    }

    public void deactivate()
    {

    }

    public void activate()
    {
        active = true;
        publication = publicationSupplier.get();
    }

    public T getPublisher()
    {
        return publisher;
    }

    public void bindPublisher(final T publisher)
    {
        this.publisher = publisher;
    }
}
