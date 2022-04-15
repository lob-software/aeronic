package io.aeronic.net;

import io.aeron.cluster.client.AeronCluster;
import org.agrona.concurrent.Agent;

public class AeronClusterAgent implements Agent
{
    private final AeronCluster aeronCluster;
    private final String subscriberName;

    public AeronClusterAgent(final AeronCluster aeronCluster, final String subscriberName)
    {
        this.aeronCluster = aeronCluster;
        this.subscriberName = subscriberName;
    }

    @Override
    public int doWork()
    {
        return aeronCluster.pollEgress();
    }

    @Override
    public String roleName()
    {
        return subscriberName;
    }

    @Override
    public void onClose()
    {
        aeronCluster.close();
    }
}
