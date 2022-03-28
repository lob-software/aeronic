package io.aeronic;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.CompositeAgent;
import org.agrona.concurrent.NoOpIdleStrategy;

import java.util.ArrayList;
import java.util.List;

public class AeronicWizard
{
    private final Aeron aeron;
    private final List<Publication> publications = new ArrayList<>();
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final List<Agent> subscriptionAgents = new ArrayList<>();
    private AgentRunner compositeAgentRunner;

    public AeronicWizard(final Aeron aeron)
    {
        this.aeron = aeron;
    }

    public <T> T createPublisher(final Class<T> clazz, final String channel, final int streamId)
    {
        final Publication publication = aeron.addPublication(channel, streamId);
        return createPublisher(clazz, publication);
    }

    @SuppressWarnings("unchecked")
    public  <T> T createPublisher(final Class<T> clazz, final Publication publication)
    {
        try
        {
            publications.add(publication);
            return (T) Class.forName(clazz.getName() + "Publisher").getConstructor(Publication.class).newInstance(publication);
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public <T> void registerSubscriber(final Class<T> aeronicInterfaceClass, final T subscriberImplementation, final String channel, final int streamId)
    {
        try
        {
            final Subscription subscription = aeron.addSubscription(channel, streamId);
            subscriptions.add(subscription);

            final Agent subscriptionAgent = (Agent) Class.forName(aeronicInterfaceClass.getName() + "Subscriber")
                .getConstructor(Subscription.class, aeronicInterfaceClass)
                .newInstance(subscription, subscriberImplementation);

            subscriptionAgents.add(subscriptionAgent);
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
        compositeAgentRunner.close();
    }

    public boolean allConnected()
    {
        return publications.stream().allMatch(Publication::isConnected)
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
