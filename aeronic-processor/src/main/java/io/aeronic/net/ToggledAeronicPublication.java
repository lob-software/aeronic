package io.aeronic.net;

import io.aeron.Publication;
import org.agrona.DirectBuffer;

import java.util.function.Supplier;

public class ToggledAeronicPublication<T> implements AeronicPublication
{
    private final Supplier<Publication> publicationSupplier;
    private Publication publication;
    private T publisher;

    public ToggledAeronicPublication(final Supplier<Publication> publicationSupplier)
    {
        this.publicationSupplier = publicationSupplier;
    }

    @Override
    public boolean isConnected()
    {
        return publication != null && publication.isConnected();
    }

    @Override
    public long offer(final DirectBuffer buffer)
    {
        return publication.offer(buffer);
    }

    @Override
    public void close()
    {
        if (publication != null) {
            publication.close();
        }
    }

    public void activate()
    {
        publication = publicationSupplier.get();
    }

    public void deactivate()
    {
        publication = null;
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
