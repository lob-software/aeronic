package benchmarks;

import io.aeronic.Aeronic;

@Aeronic
interface EchoResponse
{
    void onEchoResponse(long time, int runIdx);
}
