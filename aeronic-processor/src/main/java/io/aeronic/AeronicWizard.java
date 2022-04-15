package io.aeronic;

import io.aeron.Aeron;
import io.aeron.Subscription;
import io.aeron.cluster.client.AeronCluster;
import io.aeronic.cluster.AeronClusterPublication;
import io.aeronic.cluster.AeronClusterPublicationAgent;
import io.aeronic.cluster.AeronicCredentialsSupplier;
import io.aeronic.cluster.ClientSessionPublication;
import io.aeronic.net.*;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.CompositeAgent;

import java.util.ArrayList;
import java.util.List;

public class AeronicWizard
{
    private final Aeron aeron;
    private final List<AeronicPublication> publications = new ArrayList<>();
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final List<Agent> agents = new ArrayList<>();
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
        final AeronClusterPublication publication = new AeronClusterPublication(
            publisherName,
            new AeronCluster.Context()
                .errorHandler(Throwable::printStackTrace)
                .ingressChannel(ingressChannel)
                .aeronDirectoryName(aeron.context().aeronDirectoryName())
        );

        agents.add(new AeronClusterPublicationAgent(publication, publisherName));
        publications.add(publication);
        return createPublisher(clazz, publication);
    }

    public <T> T createClusterIngressPublisher(final Class<T> clazz, final AeronCluster.Context aeronClusterCtx)
    {
        final String publisherName = clazz.getName() + "__IngressPublisher";
        final AeronClusterPublication publication = new AeronClusterPublication(publisherName, aeronClusterCtx);

        agents.add(new AeronClusterPublicationAgent(publication, publisherName));
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

    public <T> void registerSubscriber(final Class<T> clazz, final T subscriberImplementation, final String channel, final int streamId)
    {
        final Subscription subscription = aeron.addSubscription(channel, streamId);
        subscriptions.add(subscription);

        final AbstractSubscriberInvoker<T> invoker = createSubscriberInvoker(clazz, subscriberImplementation);
        agents.add(new SubscriptionAgent<>(subscription, invoker));
    }

    public <T> void registerClusterEgressSubscriber(final Class<T> clazz, final T subscriberImplementation, final String ingressChannel)
    {
        final String subscriberName = clazz.getName() + "__EgressSubscriber";
        final AbstractSubscriberInvoker<T> invoker = createSubscriberInvoker(clazz, subscriberImplementation);

        final AeronCluster aeronCluster = AeronCluster.connect(
            new AeronCluster.Context()
                .credentialsSupplier(new AeronicCredentialsSupplier(subscriberName))
                .ingressChannel(ingressChannel)
                .egressListener((clusterSessionId, timestamp, buffer, offset, length, header) -> invoker.handle(buffer, offset))
                .errorHandler(Throwable::printStackTrace)
                .aeronDirectoryName(aeron.context().aeronDirectoryName()));

        agents.add(new AeronClusterAgent(aeronCluster, subscriberName));
    }

    public <T> void registerClusterEgressSubscriber(final Class<T> clazz, final T subscriberImplementation, final AeronCluster.Context aeronClusterCtx)
    {
        final String subscriberName = clazz.getName() + "__EgressSubscriber";
        final AbstractSubscriberInvoker<T> invoker = createSubscriberInvoker(clazz, subscriberImplementation);
        final AeronCluster aeronCluster = AeronCluster.connect(
            aeronClusterCtx
                .credentialsSupplier(new AeronicCredentialsSupplier(subscriberName))
                .egressListener((clusterSessionId, timestamp, buffer, offset, length, header) -> invoker.handle(buffer, offset))
        );
        agents.add(new AeronClusterAgent(aeronCluster, subscriberName));
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
            BusySpinIdleStrategy.INSTANCE,
            Throwable::printStackTrace,
            null,
            new CompositeAgent(agents)
        );

        AgentRunner.startOnThread(compositeAgentRunner);
    }

    public void close()
    {
        if (compositeAgentRunner != null)
        {
            compositeAgentRunner.close();
        }
        publications.forEach(AeronicPublication::close);
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
