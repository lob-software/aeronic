package io.aeronic.system.cluster;

import io.aeron.ChannelUriStringBuilder;

public class MdcCastMultiNodeClusterSystemTest extends MultiNodeClusterSystemTestBase
{
    private static final String MDC_CAST_CHANNEL = new ChannelUriStringBuilder()
            .media("udp")
            .reliable(true)
            .controlEndpoint("localhost:40458")
            .controlMode("dynamic")
            .endpoint("localhost:40459")
            .build();

    @Override
    protected String egressChannel()
    {
        return MDC_CAST_CHANNEL;
    }


}
