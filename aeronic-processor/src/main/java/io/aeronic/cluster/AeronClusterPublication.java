package io.aeronic.cluster;

import io.aeron.cluster.client.AeronCluster;
import io.aeronic.net.AeronicPublication;
import org.agrona.DirectBuffer;

public class AeronClusterPublication implements AeronicPublication
{

    private final AeronCluster aeronCluster;

    public AeronClusterPublication(final String publisherName, final AeronCluster.Context aeronClusterCtx)
    {
        this.aeronCluster = AeronCluster.connect(
            aeronClusterCtx
                .credentialsSupplier(new AeronicCredentialsSupplier(publisherName)));
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
