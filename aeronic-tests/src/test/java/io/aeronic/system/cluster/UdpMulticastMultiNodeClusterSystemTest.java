package io.aeronic.system.cluster;

import io.aeron.ChannelUriStringBuilder;

public class UdpMulticastMultiNodeClusterSystemTest extends MultiNodeClusterSystemTestBase
{
    private static final String UDP_MULTICAST_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .endpoint("224.0.1.1:40457")
        .networkInterface("localhost")
        .build();

    @Override
    protected String egressChannel()
    {
        return UDP_MULTICAST_CHANNEL;
    }
}
