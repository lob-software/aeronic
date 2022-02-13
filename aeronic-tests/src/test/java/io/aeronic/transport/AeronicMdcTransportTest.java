package io.aeronic.transport;

import io.aeron.ChannelUriStringBuilder;

public class AeronicMdcTransportTest extends AeronicTransportTestBase
{
    @Override
    public String getPublicationChannel()
    {
        return new ChannelUriStringBuilder()
            .media("udp")
            .reliable(true)
            .controlEndpoint("localhost:40456")
            .controlMode("dynamic")
            .endpoint("localhost:40457")
            .build();
    }

    @Override
    public String getSubscriptionChannel()
    {
        return new ChannelUriStringBuilder()
            .media("udp")
            .reliable(true)
            .controlEndpoint("localhost:40456")
            .controlMode("dynamic")
            .endpoint("localhost:40455")
            .build();
    }
}
