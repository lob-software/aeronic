package io.aeronic.transport;

import io.aeron.ChannelUriStringBuilder;

public class AeronicUdpUnicastTransportTest extends AeronicTransportTestBase
{

    // some test cases do not make sense in the real world but pass by virtue of being run locally

    public static final String UNICAST_CHANNEL = new ChannelUriStringBuilder()
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
