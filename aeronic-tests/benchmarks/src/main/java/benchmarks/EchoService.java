package benchmarks;

public class EchoService implements Echo {
    private final EchoResponse echoResponseProxy;

    public EchoService(final EchoResponse echoResponseProxy)
    {
        this.echoResponseProxy = echoResponseProxy;
    }

    @Override
    public void echo(final long time, final int runIdx)
    {
        echoResponseProxy.onEchoResponse(time, runIdx);
    }
}
