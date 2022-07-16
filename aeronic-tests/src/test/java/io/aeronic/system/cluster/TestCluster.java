package io.aeronic.system.cluster;

import io.aeron.cluster.ClusterTool;
import io.aeron.cluster.service.Cluster;
import io.aeronic.cluster.AeronicClusteredServiceContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static io.aeronic.Assertions.assertEventuallyTrue;
import static org.awaitility.Awaitility.await;

public class TestCluster
{
    private final List<AeronicClusteredServiceContainer> clusteredServices = new ArrayList<>();
    private final List<TestClusterNode> clusterNodes = new ArrayList<>();

    public void registerNode(final int nodeIdx, final int nodeCount, final AeronicClusteredServiceContainer clusteredServiceContainer)
    {
        final TestClusterNode clusterNode = new TestClusterNode(nodeIdx, nodeCount, clusteredServiceContainer);
        clusteredServices.add(clusteredServiceContainer);
        clusterNodes.add(clusterNode);
    }

    public AeronicClusteredServiceContainer waitForLeader()
    {
        final Optional<AeronicClusteredServiceContainer> leaderMaybe = await()
            .until(
                () -> clusteredServices.stream()
                    .filter(e -> e.getRole() == Cluster.Role.LEADER)
                    .findFirst(),
                Optional::isPresent
            );

        return leaderMaybe.orElseThrow();
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
    }

    public void removeLeader()
    {
        final AeronicClusteredServiceContainer leader = waitForLeader();
        final int leaderIdx = clusteredServices.indexOf(leader);
        final TestClusterNode leaderNode = clusterNodes.get(leaderIdx);

        if (!ClusterTool.removeMember(leaderNode.clusterDir(), leaderIdx, false))
        {
            throw new IllegalStateException("could not remove member");
        }

        leaderNode.close();

        clusteredServices.remove(leaderIdx);
        clusterNodes.remove(leaderIdx);

        assertEventuallyTrue(() -> leader != waitForLeader(), 10_000);
    }
}
