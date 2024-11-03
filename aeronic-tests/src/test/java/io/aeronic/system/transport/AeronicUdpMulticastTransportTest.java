package io.aeronic.system.transport;

import io.aeron.ChannelUriStringBuilder;

public class AeronicUdpMulticastTransportTest extends AeronicTransportTestBase
{

    private static final String MULTICAST_CHANNEL = new ChannelUriStringBuilder()
            .media("udp")
            .reliable(true)
            .endpoint("224.0.1.1:40457")
            .networkInterface("localhost")
            .build();

    @Override
    public String getPublicationChannel()
    {
        return MULTICAST_CHANNEL;
    }

    @Override
    public String getSubscriptionChannel()
    {
        return MULTICAST_CHANNEL;
    }
}
