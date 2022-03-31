package io.aeronic.net;

import io.aeron.cluster.client.AeronCluster;
import org.agrona.DirectBuffer;

public class ClusterPublication implements AeronicPublication
{

    private final AeronCluster aeronCluster;

    public ClusterPublication(final AeronCluster aeronCluster)
    {
        this.aeronCluster = aeronCluster;
    }

    @Override
    public boolean isConnected()
    {
        return aeronCluster.ingressPublication().isConnected();
    }

    @Override
    public void offer(final DirectBuffer buffer)
    {
        aeronCluster.offer(buffer, 0, buffer.capacity());
    }
}