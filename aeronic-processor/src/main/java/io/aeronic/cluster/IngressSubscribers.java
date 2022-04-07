package io.aeronic.cluster;

import io.aeronic.AeronicWizard;
import io.aeronic.net.AbstractSubscriberInvoker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IngressSubscribers
{

    private final List<AbstractSubscriberInvoker<?>> subscriberInvokers = new ArrayList<>();

    public void forEach(final Consumer<AbstractSubscriberInvoker<?>> subscriberInvokerConsumer)
    {
        for (final AbstractSubscriberInvoker<?> subscriberInvoker : subscriberInvokers)
        {
            subscriberInvokerConsumer.accept(subscriberInvoker);
        }
    }

    public <T> void register(final Class<T> clazz, final T subscriberImplementation)
    {
        subscriberInvokers.add(AeronicWizard.createSubscriberInvoker(clazz, subscriberImplementation));
    }
}
