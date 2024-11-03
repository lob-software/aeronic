package io.aeronic.net;

import io.aeronic.codec.BufferDecoder;
import org.agrona.DirectBuffer;

public abstract class AbstractSubscriberInvoker<T>
{
    protected T subscriber;
    protected final BufferDecoder bufferDecoder = new BufferDecoder();

    public AbstractSubscriberInvoker(final T subscriber)
    {
        this.subscriber = subscriber;
    }

    public AbstractSubscriberInvoker()
    {
    }

    public T getSubscriber()
    {
        return subscriber;
    }

    protected abstract void handle(final BufferDecoder bufferDecoder, final int offset);

    public void handle(final DirectBuffer buffer, final int offset)
    {
        bufferDecoder.wrap(buffer, offset);
        handle(bufferDecoder, offset);
    }
}
