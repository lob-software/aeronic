package io.aeronic;

import io.aeron.Aeron;
import io.aeron.Subscription;
import io.aeron.cluster.client.AeronCluster;
import io.aeronic.cluster.AeronClusterPublication;
import io.aeronic.cluster.AeronicCredentialsSupplier;
import io.aeronic.cluster.ClientSessionPublication;
import io.aeronic.net.AbstractSubscriberAgent;
import io.aeronic.net.AbstractSubscriberInvoker;
import io.aeronic.net.AeronicPublication;
import io.aeronic.net.SimplePublication;
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
        publications.add(publication);
        return createPublisher(clazz, publication);
    }

    public <T> T createClusterIngressPublisher(final Class<T> clazz, final String ingressChannel)
    {
        final String publisherName = clazz.getName() + "__IngressPublisher";
        final AeronicPublication publication = new AeronClusterPublication(publisherName, ingressChannel, aeron.context().aeronDirectoryName());
        publications.add(publication);
        return createPublisher(clazz, publication);
    }

    public static <T> ClientSessionPublication<T> createClusterEgressPublisher(final Class<T> clazz)
    {
        final String publisherName = clazz.getName() + "__EgressPublisher";
        final ClientSessionPublication<T> publication = new ClientSessionPublication<>(publisherName);
        final T publisher = createPublisher(clazz, publication);
        publication.bindPublisher(publisher);
        return publication;
    }

    @SuppressWarnings("unchecked")
    public static <T> T createPublisher(final Class<T> clazz, final AeronicPublication publication)
    {
        try
        {
            return (T)Class.forName(clazz.getName() + "Publisher").getConstructor(AeronicPublication.class).newInstance(publication);
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public <T> AeronCluster registerClusterEgressSubscriber(final Class<T> clazz, final T subscriberImplementation, final String ingressChannel)
    {
        final String subscriberName = clazz.getName() + "__EgressSubscriber";
        final AbstractSubscriberInvoker<T> subscriberInvoker = createSubscriberInvoker(clazz, subscriberImplementation);

        return AeronCluster.connect(
            new AeronCluster.Context()
                .credentialsSupplier(new AeronicCredentialsSupplier(subscriberName))
                .ingressChannel(ingressChannel)
                .egressListener((clusterSessionId, timestamp, buffer, offset, length, header) -> subscriberInvoker.handle(buffer, offset))
                .errorHandler(Throwable::printStackTrace)
                .aeronDirectoryName(aeron.context().aeronDirectoryName()));
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
