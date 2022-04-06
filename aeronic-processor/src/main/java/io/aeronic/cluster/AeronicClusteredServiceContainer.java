package io.aeronic.cluster;

import io.aeron.ExclusivePublication;
import io.aeron.Image;
import io.aeron.cluster.codecs.CloseReason;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeron.cluster.service.ClusteredService;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;

import java.util.concurrent.TimeUnit;

public class AeronicClusteredServiceContainer implements ClusteredService
{
    private final ClusteredService clusteredService;
    private final AeronicClusteredServiceRegistry registry = new AeronicClusteredServiceRegistry();

    public AeronicClusteredServiceContainer(
        final ClusteredService clusteredService,
        final IngressSubscribers ingressSubscribers,
        final EgressPublishers egressPublishers
    )
    {
        this.clusteredService = clusteredService;
        ingressSubscribers.forEach(registry::registerIngressSubscriberInvoker);
        egressPublishers.forEach(registry::registerEgressPublisher);
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
