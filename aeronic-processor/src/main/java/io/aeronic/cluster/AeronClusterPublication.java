package io.aeronic.cluster;

import io.aeron.cluster.client.AeronCluster;
import io.aeronic.net.AeronicPublication;
import org.agrona.DirectBuffer;

public class AeronClusterPublication implements AeronicPublication
{

    private final AeronCluster aeronCluster;

    public AeronClusterPublication(final String publisherName, final String ingressChannel, final String aeronDirectoryName)
    {
        this.aeronCluster = AeronCluster.connect(
            new AeronCluster.Context()
                .credentialsSupplier(new AeronicCredentialsSupplier(publisherName))
                .ingressChannel(ingressChannel)
                .errorHandler(Throwable::printStackTrace)
                .aeronDirectoryName(aeronDirectoryName));
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
