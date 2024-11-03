package io.aeronic.net;

import io.aeronic.codec.BufferDecoder;

public class NullSubscriberInvokerImpl<T> extends AbstractSubscriberInvoker<T>
{
    public static final NullSubscriberInvokerImpl<?> INSTANCE = new NullSubscriberInvokerImpl<>(null);

    private NullSubscriberInvokerImpl(final T subscriber)
    {
        super(subscriber);
    }

    @Override
    protected void handle(final BufferDecoder bufferDecoder, final int offset)
    {

    }
}
