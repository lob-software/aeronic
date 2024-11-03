package io.aeronic.test;

import io.aeronic.Aeronic;
import io.aeronic.net.AbstractSubscriberInvoker;

import java.util.HashMap;

import static io.aeronic.AeronicImpl.createSubscriberInvoker;

public class TestAeronic implements Aeronic
{

    private final HashMap<Class<?>, AbstractSubscriberInvoker<?>> clazzToSubscriberMap = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T createPublisher(final Class<T> clazz, final String channel, final int streamId)
    {
        final AbstractSubscriberInvoker<?> subscriberInvoker = clazzToSubscriberMap.get(clazz);
        if (subscriberInvoker == null) {
            throw new IllegalStateException(("Cannot create a publisher before registering a subscriber! " +
                    "Register %s subscriber first.").formatted(clazz.getName()));
        }
        return (T) subscriberInvoker.getSubscriber();
    }

    @Override
    public <T> void registerSubscriber(final Class<T> clazz, final T subscriberImplementation, final String channel, final int streamId)
    {
        clazzToSubscriberMap.put(clazz, createSubscriberInvoker(clazz, subscriberImplementation));
    }
}
