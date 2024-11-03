package io.aeronic.net;

import org.agrona.DirectBuffer;

public interface AeronicPublication
{
    boolean isConnected();

    long offer(DirectBuffer buffer);

    void close();

    default void onOfferFailure(long offerResult)
    {

    }
}
