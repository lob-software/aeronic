package io.aeronic;

import io.aeron.Aeron;
import io.aeron.Subscription;
import io.aeron.cluster.client.AeronCluster;
import io.aeronic.net.*;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.CompositeAgent;
import org.agrona.concurrent.NoOpIdleStrategy;

import java.util.ArrayList;
import java.util.List;

public class AeronicWizard
{
    private final Aeron aeron;
    private final List<AeronicPublication> publications = new ArrayList<>();
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final List<Agent> subscriptionAgents = new ArrayList<>();
    private AgentRunner compositeAgentRunner;

    public AeronicWizard(final Aeron aeron)
    {
        this.aeron = aeron;
    }

    public <T> T createPublisher(final Class<T> clazz, final String channel, final int streamId)
    {
        final AeronicPublication publication = new SimplePublication(aeron.addPublication(channel, streamId));
        return createPublisher(clazz, publication);
    }

    public <T> T createClusterPublisher(final Class<T> clazz, final AeronCluster aeronCluster)
    {
        final AeronicPublication publication = new ClusterPublication(aeronCluster);
        return createPublisher(clazz, publication);
    }

    @SuppressWarnings("unchecked")
    public <T> T createPublisher(final Class<T> clazz, final AeronicPublication publication)
    {
        try
        {
            publications.add(publication);
            return (T)Class.forName(clazz.getName() + "Publisher").getConstructor(AeronicPublication.class).newInstance(publication);
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public <T> void registerSubscriber(final Class<T> clazz, final T subscriberImplementation, final String channel, final int streamId)
    {
        final Subscription subscription = aeron.addSubscription(channel, streamId);
        subscriptions.add(subscription);

        final AbstractSubscriberInvoker<T> invoker = createSubscriberInvoker(clazz, subscriberImplementation);
        subscriptionAgents.add(new AbstractSubscriberAgent<>(subscription, invoker));
    }

    @SuppressWarnings("unchecked")
    public static <T> AbstractSubscriberInvoker<T> createSubscriberInvoker(final Class<T> clazz, final T subscriberImplementation)
    {
        try
        {
            return (AbstractSubscriberInvoker<T>)Class.forName(clazz.getName() + "Invoker")
                .getConstructor(clazz)
                .newInstance(subscriberImplementation);
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void start()
    {
        compositeAgentRunner = new AgentRunner(
            NoOpIdleStrategy.INSTANCE,
            Throwable::printStackTrace,
            null,
            new CompositeAgent(subscriptionAgents)
        );

        AgentRunner.startOnThread(compositeAgentRunner);
    }

    public void close()
    {
        if (compositeAgentRunner != null)
        {
            compositeAgentRunner.close();
        }
    }

    public boolean allConnected()
    {
        return publications.stream().allMatch(AeronicPublication::isConnected)
            && subscriptions.stream().allMatch(Subscription::isConnected);
    }

    public void awaitUntilPubsAndSubsConnect()
    {
        while (!allConnected())
        {
            aeron.context().idleStrategy().idle();
        }
    }
}
