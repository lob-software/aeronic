package io.aeronic.cluster;

import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import io.aeron.logbuffer.Header;
import io.aeronic.AeronicWizard;
import org.agrona.DirectBuffer;

import java.util.concurrent.TimeUnit;

public class AeronicClusteredServiceContainer implements ClusteredService
{
    private final ClusteredService clusteredService;
    private final AeronicClusteredServiceRegistry registry;

    private AeronicClusteredServiceContainer(
        final ClusteredService clusteredService,
        final AeronicClusteredServiceRegistry registry
    )
    {
        this.clusteredService = clusteredService;
        this.registry = registry;
    }

    public static Configuration configure()
    {
        return new Configuration();
    }

    public <T> T getPublisherFor(final Class<T> clazz)
    {
        return registry.getPublisherFor(clazz);
    }

    public boolean egressConnected()
    {
        return registry.egressConnected();
    }

    public void close()
    {
        registry.close();
    }

    public static class Configuration
    {
        private ClusteredService clusteredService;
        private final AeronicClusteredServiceRegistry registry = new AeronicClusteredServiceRegistry();

        public Configuration clusteredService(final ClusteredService clusteredService)
        {
            this.clusteredService = clusteredService;
            return this;
        }

        public <T> Configuration registerIngressSubscriber(final Class<T> clazz, final T subscriberImplementation)
        {
            registry.registerIngressSubscriberInvoker(AeronicWizard.createSubscriberInvoker(clazz, subscriberImplementation));
            return this;
        }

        public <T> Configuration registerEgressPublisher(final Class<T> clazz)
        {
            registry.registerEgressPublisher(AeronicWizard.createClusterEgressPublisher(clazz));
            return this;
        }

        public AeronicClusteredServiceRegistry registry()
        {
            return registry;
        }

        public AeronicClusteredServiceContainer create()
        {
            return new AeronicClusteredServiceContainer(clusteredService, registry);
        }
    }

    @Override
    public void onStart(final Cluster cluster, final Image snapshotImage)
    {
        clusteredService.onStart(cluster, snapshotImage);
    }

    @Override
    public void onSessionOpen(final ClientSession session, final long timestamp)
    {
        registry.onSessionOpen(session);
        clusteredService.onSessionOpen(session, timestamp);
    }

    @Override
    public void onSessionClose(final ClientSession session, final long timestamp, final CloseReason closeReason)
    {
        clusteredService.onSessionClose(session, timestamp, closeReason);
    }

    @Override
    public void onSessionMessage(
        final ClientSession session,
        final long timestamp,
        final DirectBuffer buffer,
        final int offset,
        final int length,
        final Header header
    )
    {
        registry.onSessionMessage(session, buffer, offset);
        clusteredService.onSessionMessage(session, timestamp, buffer, offset, length, header);
    }

    @Override
    public void onTimerEvent(final long correlationId, final long timestamp)
    {
        clusteredService.onTimerEvent(correlationId, timestamp);
    }

    @Override
    public void onTakeSnapshot(final ExclusivePublication snapshotPublication)
    {
        clusteredService.onTakeSnapshot(snapshotPublication);
    }

    @Override
    public void onRoleChange(final Cluster.Role newRole)
    {
        clusteredService.onRoleChange(newRole);
    }

    @Override
    public void onTerminate(final Cluster cluster)
    {
        clusteredService.onTerminate(cluster);
    }

    @Override
    public void onNewLeadershipTermEvent(
        final long leadershipTermId,
        final long logPosition,
        final long timestamp,
        final long termBaseLogPosition,
        final int leaderMemberId,
        final int logSessionId,
        final TimeUnit timeUnit,
        final int appVersion
    )
    {
        clusteredService.onNewLeadershipTermEvent(
            leadershipTermId,
            logPosition,
            timestamp,
            termBaseLogPosition,
            leaderMemberId,
            logSessionId,
            timeUnit,
            appVersion
        );
    }
}
