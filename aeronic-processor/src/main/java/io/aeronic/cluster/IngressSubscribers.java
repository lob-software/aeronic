package io.aeronic.cluster;

import io.aeronic.net.AbstractSubscriberInvoker;

import java.util.function.Consumer;

public class IngressSubscribers
{

    private final AbstractSubscriberInvoker<?>[] subscriberInvokers;

    private IngressSubscribers()
    {
        this.subscriberInvokers = new AbstractSubscriberInvoker[0];
    }

    private IngressSubscribers(final AbstractSubscriberInvoker<?>[] subscriberInvokers)
    {
        this.subscriberInvokers = subscriberInvokers;
    }

    public static IngressSubscribers create(final AbstractSubscriberInvoker<?>... subscriberInvokers)
    {
        return new IngressSubscribers(subscriberInvokers);
    }

    public static IngressSubscribers none()
    {
        return new IngressSubscribers();
    }

    public void forEach(final Consumer<AbstractSubscriberInvoker<?>> subscriberInvokerConsumer)
    {
        for (final AbstractSubscriberInvoker<?> subscriberInvoker : subscriberInvokers)
        {
            subscriberInvokerConsumer.accept(subscriberInvoker);
        }
    }
}
