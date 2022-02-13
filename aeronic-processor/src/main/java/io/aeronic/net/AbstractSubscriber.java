package io.aeronic.net;

import io.aeron.Subscription;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;

public abstract class AbstractSubscriber<T> implements Agent
{
    private final Subscription subscription;
    protected final T subscriber;
    protected final BufferDecoder bufferDecoder = new BufferDecoder();

    public AbstractSubscriber(final Subscription subscription, final T subscriber)
    {
        this.subscription = subscription;
        this.subscriber = subscriber;
    }

    @Override
    public int doWork()
    {
        return subscription.poll(this::handle, 1000);
    }

    protected abstract void handle(final BufferDecoder bufferDecoder, final int offset);

    private void handle(final DirectBuffer buffer, final int offset, final int length, final Header header)
    {
        bufferDecoder.wrap(buffer, offset);
        handle(bufferDecoder, offset);
    }
}
