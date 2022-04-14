package io.aeronic.net;

import org.agrona.DirectBuffer;

public interface AeronicPublication
{
    boolean isConnected();
    void offer(DirectBuffer buffer);
    void close();
}
