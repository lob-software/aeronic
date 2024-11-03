package io.aeronic.system.cluster;

import io.aeron.cluster.service.Cluster;
import io.aeronic.cluster.AeronicClusteredServiceContainer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static io.aeronic.system.cluster.TestClusterNode.INGRESS_CHANNEL;
import static org.awaitility.Awaitility.await;

public class TestCluster
{
    private final List<AeronicClusteredServiceContainer> clusteredServices = new ArrayList<>();
    private final List<TestClusterNode> clusterNodes = new ArrayList<>();

    public void registerNode(
            final int nodeIdx,
            final int nodeCount,
            final AeronicClusteredServiceContainer clusteredServiceContainer
                            )
    {
        final TestClusterNode clusterNode = new TestClusterNode(
                new TestClusterNode.Context()
                        .nodeId(nodeIdx)
                        .nodeCount(nodeCount)
                        .clusteredService(clusteredServiceContainer)
                        .ingressChannel(INGRESS_CHANNEL)
                        .deleteDirs(false));

        clusteredServices.add(nodeIdx, clusteredServiceContainer);
        clusterNodes.add(nodeIdx, clusterNode);
    }

    public AeronicClusteredServiceContainer restartNode(final int nodeIdx, final int nodeCount)
    {
        final AeronicClusteredServiceContainer clusteredServiceContainer = clusteredServices.get(nodeIdx);
        final TestClusterNode clusterNode = new TestClusterNode(nodeIdx, nodeCount, clusteredServiceContainer);
        clusterNodes.set(nodeIdx, clusterNode);
        return clusteredServiceContainer;
    }

    public AeronicClusteredServiceContainer waitForLeader()
    {
        // CAREFUL! this method can retrieve stale leader, e.g. if called after failover
        final Optional<AeronicClusteredServiceContainer> leaderMaybe = await()
                .until(
                        () -> clusteredServices.stream()
                                .filter(e -> e.getRole() == Cluster.Role.LEADER)
                                .findFirst(),
                        Optional::isPresent
                      );

        return leaderMaybe.orElseThrow();
    }

    public AeronicClusteredServiceContainer waitForLeader(final int skipNodeIdx, final long millis)
    {
        return await()
                .timeout(Duration.ofMillis(millis))
                .until(
                        () -> {
                            for (int i = 0; i < clusteredServices.size(); i++) {
                                final AeronicClusteredServiceContainer clusteredService = clusteredServices.get(i);
                                if (i != skipNodeIdx && clusteredService.getRole() == Cluster.Role.LEADER) {
                                    return clusteredService;
                                }
                            }
                            return null;
                        },
                        Objects::nonNull
                      );
    }

    public void forEachNonLeaderNode(final Consumer<? super AeronicClusteredServiceContainer> consumer)
    {
        clusteredServices.stream()
                .filter(e -> e.getRole() != Cluster.Role.LEADER)
                .forEach(consumer);
    }

    public void close()
    {
        clusterNodes.forEach(TestClusterNode::close);
        clusteredServices.forEach(AeronicClusteredServiceContainer::close);
        clusterNodes.forEach(TestClusterNode::deleteDirs);
    }

    public AeronicClusteredServiceContainer shutdownLeader()
    {
        final AeronicClusteredServiceContainer leader = waitForLeader();
        final int leaderIdx = clusteredServices.indexOf(leader);
        final TestClusterNode leaderNode = clusterNodes.get(leaderIdx);

        leaderNode.close();

        return waitForLeader(leaderIdx, 20_000L);
    }

    public int getNodeIdx(final AeronicClusteredServiceContainer leaderClusteredService)
    {
        return clusteredServices.indexOf(leaderClusteredService);
    }
}
