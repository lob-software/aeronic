package io.aeronic;

public interface Aeronic
{
    <T> T createPublisher(Class<T> clazz, String channel, int streamId);

    <T> void registerSubscriber(Class<T> clazz, T subscriberImplementation, String channel, int streamId);
}
