package io.aeronic.net;

import io.aeron.Publication;
import org.agrona.DirectBuffer;

import java.util.function.LongConsumer;

public class SimplePublication implements AeronicPublication
{

    private final Publication publication;
    private final LongConsumer offerFailureHandler;

    public SimplePublication(final Publication publication, final LongConsumer offerFailureHandler)
    {
        this.publication = publication;
        this.offerFailureHandler = offerFailureHandler;
    }

    @Override
    public boolean isConnected()
    {
        return publication.isConnected();
    }

    @Override
    public long offer(final DirectBuffer buffer)
    {
        return publication.offer(buffer);
    }

    @Override
    public void close()
    {
        if (publication.isConnected())
        {
            publication.close();
        }
    }

    @Override
    public void onOfferFailure(final long offerResult)
    {
        offerFailureHandler.accept(offerResult);
    }
}
