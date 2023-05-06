package io.aeronic;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.cluster.client.AeronCluster;
import io.aeronic.cluster.AeronClusterPublication;
import io.aeronic.cluster.AeronClusterPublicationAgent;
import io.aeronic.cluster.AeronicCredentialsSupplier;
import io.aeronic.cluster.ClientSessionPublication;
import io.aeronic.net.*;
import org.agrona.ErrorHandler;
import org.agrona.concurrent.*;
import org.agrona.concurrent.status.AtomicCounter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongConsumer;

import static org.awaitility.Awaitility.await;

public class AeronicWizard implements AutoCloseable
{
    private final Aeron aeron;
    private final IdleStrategy idleStrategy;
    private final ErrorHandler errorHandler;
    private final AtomicCounter errorCounter;
    private final Function<List<Agent>, Agent> multipleAgentsTransformer;

    private final List<AeronicPublication> publications = new ArrayList<>();
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final List<Agent> agents = new ArrayList<>();
    private final LongConsumer offerFailureHandler;
    private AgentRunner agentRunner;

    public AeronicWizard(final Aeron aeron)
    {
        this(
            new Context()
                .aeron(aeron)
                .idleStrategy(NoOpIdleStrategy.INSTANCE)
                .errorHandler(Throwable::printStackTrace)
                .atomicCounter(null)
                .agentSupplier(CompositeAgent::new)
        );
    }

    public AeronicWizard(final Context ctx)
    {
        this.aeron = ctx.aeron;
        this.idleStrategy = ctx.idleStrategy;
        this.errorHandler = ctx.errorHandler;
        this.errorCounter = ctx.atomicCounter;
        this.multipleAgentsTransformer = ctx.agentSupplier;
        this.offerFailureHandler = ctx.offerFailureHandler;
    }

    public static class Context
    {
        private Aeron aeron;
        private IdleStrategy idleStrategy;
        private ErrorHandler errorHandler;
        private AtomicCounter atomicCounter;
        private Function<List<Agent>, Agent> agentSupplier;
        private LongConsumer offerFailureHandler = f -> {};

        public Context aeron(final Aeron aeron)
        {
            this.aeron = aeron;
            return this;
        }

        public Context idleStrategy(final IdleStrategy idleStrategy)
        {
            this.idleStrategy = idleStrategy;
            return this;
        }

        public Context errorHandler(final ErrorHandler errorHandler)
        {
            this.errorHandler = errorHandler;
            return this;
        }

        public Context atomicCounter(final AtomicCounter atomicCounter)
        {
            this.atomicCounter = atomicCounter;
            return this;
        }

        public Context agentSupplier(final Function<List<Agent>, Agent> agentSupplier)
        {
            this.agentSupplier = agentSupplier;
            return this;
        }

        public Context offerFailureHandler(final LongConsumer offerFailureHandler)
        {
            this.offerFailureHandler = offerFailureHandler;
            return this;
        }
    }

    public <T> T createPublisher(final Class<T> clazz, final String channel, final int streamId)
    {
        final Publication rawPublication = aeron.addPublication(channel, streamId);
        final AeronicPublication publication = new SimplePublication(rawPublication, offerFailureHandler);
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

    public <T> T createClusterIngressPublisher(final Class<T> clazz, final String ingressChannel)
    {
        return createClusterIngressPublisher(
            clazz,
            new AeronCluster.Context()
                .errorHandler(Throwable::printStackTrace)
                .ingressChannel(ingressChannel)
                .aeronDirectoryName(aeron.context().aeronDirectoryName())
        );
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
        agentRunner = new AgentRunner(
            idleStrategy,
            errorHandler,
            errorCounter,
            multipleAgentsTransformer.apply(agents)
        );

        AgentRunner.startOnThread(agentRunner);
    }

    @Override
    public void close()
    {
        if (agentRunner != null)
        {
            agentRunner.close();
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
        await()
            .timeout(Duration.ofSeconds(5))
            .until(() -> {
                final boolean allConnected = allConnected();
                aeron.context().idleStrategy().idle();
                return allConnected;
            });
    }
}
