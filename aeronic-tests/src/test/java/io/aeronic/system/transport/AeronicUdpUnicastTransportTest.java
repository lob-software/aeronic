package io.aeronic.system.transport;

import io.aeron.ChannelUriStringBuilder;

public class AeronicUdpUnicastTransportTest extends AeronicTransportTestBase
{

    private static final String UNICAST_CHANNEL = new ChannelUriStringBuilder()
            .media("udp")
            .reliable(true)
            .endpoint("localhost:40457")
            .build();

    @Override
    public String getPublicationChannel()
    {
        return UNICAST_CHANNEL;
    }

    @Override
    public String getSubscriptionChannel()
    {
        return UNICAST_CHANNEL;
    }
}
