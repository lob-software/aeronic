package io.aeronic.transport;

public class AeronicIpcTransportTest extends AeronicTransportTestBase
{

    private static final String IPC = "aeron:ipc";

    @Override
    public String getPublicationChannel()
    {
        return IPC;
    }

    @Override
    public String getSubscriptionChannel()
    {
        return IPC;
    }
}
