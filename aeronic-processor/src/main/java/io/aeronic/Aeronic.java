package io.aeronic;

import io.aeron.*;
import io.aeron.archive.client.AeronArchive;
import io.aeron.archive.client.ReplayMerge;
import io.aeron.archive.codecs.SourceLocation;
import io.aeron.archive.status.RecordingPos;
import io.aeron.cluster.client.AeronCluster;
import io.aeronic.cluster.AeronClusterPublication;
import io.aeronic.cluster.AeronClusterPublicationAgent;
import io.aeronic.cluster.AeronicCredentialsSupplier;
import io.aeronic.cluster.ClientSessionPublication;
import io.aeronic.net.*;
import org.agrona.ErrorHandler;
import org.agrona.collections.MutableLong;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.DynamicCompositeAgent;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;
import org.agrona.concurrent.status.AtomicCounter;
import org.agrona.concurrent.status.CountersReader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Exchanger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import static io.aeron.Aeron.NULL_VALUE;
import static org.awaitility.Awaitility.await;

public class Aeronic implements AutoCloseable
{
    private final Aeron aeron;
    private final AeronArchive aeronArchive;
    private final IdleStrategy idleStrategy;
    private final ErrorHandler errorHandler;
    private final AtomicCounter errorCounter;

    private final List<AeronicPublication> publications = new ArrayList<>();
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final DynamicCompositeAgent dynamicCompositeAgent = new DynamicCompositeAgent("aeronic-composite");
    private final LongConsumer offerFailureHandler;
    private AgentRunner agentRunner;

    private Aeronic(final Aeronic.Context ctx)
    {
        this.aeron = ctx.aeron;
        this.aeronArchive = ctx.aeronArchive;
        this.idleStrategy = ctx.idleStrategy;
        this.errorHandler = ctx.errorHandler;
        this.errorCounter = ctx.atomicCounter;
        this.offerFailureHandler = ctx.offerFailureHandler;
    }

    public static Aeronic launch(final Context ctx)
    {
        final Aeronic aeronic = new Aeronic(ctx);
        aeronic.start();
        return aeronic;
    }

    public static class Context
    {
        private Aeron aeron;
        private AeronArchive aeronArchive;
        private IdleStrategy idleStrategy = NoOpIdleStrategy.INSTANCE;
        private ErrorHandler errorHandler = Throwable::printStackTrace;
        private AtomicCounter atomicCounter;
        private LongConsumer offerFailureHandler = f -> {};

        public Context aeron(final Aeron aeron)
        {
            this.aeron = aeron;
            return this;
        }

        public Context aeronArchive(final AeronArchive aeronArchive)
        {
            this.aeronArchive = aeronArchive;
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


    @SuppressWarnings("unchecked")
    public <T> T createTestPublisher(final Class<T> clazz)
    {
        return (T)Proxy.newProxyInstance(
            clazz.getClassLoader(),
            clazz.getInterfaces(),
            (proxy, method, args) -> {
                clazzToSubscriberMap.get(clazz).forEach(subscriberImpl -> {
                    try
                    {
                        method.invoke(subscriberImpl, args);
                    }
                    catch (final Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                });
                return null;
            }
        );
    }

    private final HashMap<Class<?>, List<?>> clazzToSubscriberMap = new HashMap<>();

    public <T> void registerTestSubscriber(final Class<T> simpleEventsClass, final T impl)
    {
        clazzToSubscriberMap.computeIfAbsent(simpleEventsClass, (List<T>) new ArrayList<>()).add(impl);
    }


    public <T> T createRecordedPublisher(final Class<T> clazz, final String channel, final int streamId)
    {
        final Publication rawPublication = aeron.addPublication(channel, streamId);
        final AeronicPublication publication = new SimplePublication(rawPublication, offerFailureHandler);
        publications.add(publication);

        aeronArchive.startRecording(channel, streamId, SourceLocation.REMOTE);
        final CountersReader counters = aeron.countersReader();
        awaitRecordingCounterId(counters, rawPublication.sessionId());

        return createPublisher(clazz, publication);
    }

    public void awaitRecordingCounterId(final CountersReader counters, final int sessionId)
    {
        while (NULL_VALUE == RecordingPos.findCounterIdBySession(counters, sessionId))
        {
            LockSupport.parkNanos(1000);
        }
    }

    public <T> T createClusterIngressPublisher(final Class<T> clazz, final AeronCluster.Context aeronClusterCtx)
    {
        final String publisherName = clazz.getName() + "__IngressPublisher";
        final AeronClusterPublication publication = new AeronClusterPublication(publisherName, aeronClusterCtx);

        dynamicCompositeAgent.tryAdd(new AeronClusterPublicationAgent(publication, publisherName));
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
        dynamicCompositeAgent.tryAdd(new SubscriptionAgent<>(subscription, invoker));
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

        dynamicCompositeAgent.tryAdd(new AeronClusterAgent(aeronCluster, subscriberName));
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
        dynamicCompositeAgent.tryAdd(new AeronClusterAgent(aeronCluster, subscriberName));
    }

    public <T> void registerPersistentSubscriber(
        final Class<T> clazz,
        final T subscriberImplementation,
        final String livePublicationAlias,
        final int streamId
    )
    {
        final String subscriptionChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .controlMode(CommonContext.MDC_CONTROL_MODE_MANUAL)
            .build();

        final String replayChannel = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .build();

        final String replayDestination = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .endpoint("localhost:0")
            .build();

        final String liveDestination = new ChannelUriStringBuilder()
            .media(CommonContext.UDP_MEDIA)
            .endpoint("localhost:23267")
            .controlEndpoint("localhost:23265")
            .build();

        final long recordingId = fetchRecordingId(livePublicationAlias);

        final Subscription subscription = aeron.addSubscription(subscriptionChannel, streamId);

        final ReplayMerge replayMerge = new ReplayMerge(
            subscription,
            aeronArchive,
            replayChannel,
            replayDestination,
            liveDestination,
            recordingId,
            AeronArchive.NULL_POSITION
        );

        final AbstractSubscriberInvoker<T> invoker = createSubscriberInvoker(clazz, subscriberImplementation);

        dynamicCompositeAgent.tryAdd(new ReplayMergeAgent<>(replayMerge, invoker));
    }

    private long fetchRecordingId(final String liveChannelAlias)
    {
        final MutableLong recordingIdRef = new MutableLong();

        aeronArchive.listRecordings(
            0, 10,
            (
                controlSessionId, correlationId, recordingId, startTimestamp, stopTimestamp, startPosition, stopPosition, initialTermId,
                segmentFileLength, termBufferLength, mtuLength, sessionId, streamId, strippedChannel, originalChannel, sourceIdentity
            ) -> {
                if (originalChannel.contains(liveChannelAlias))
                {
                    recordingIdRef.set(recordingId);
                }
            }
        );

        return recordingIdRef.get();
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

    private void start()
    {
        agentRunner = new AgentRunner(
            idleStrategy,
            errorHandler,
            errorCounter,
            dynamicCompositeAgent
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
