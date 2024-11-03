package benchmarks;

import io.aeronic.gen.Aeronic;

@Aeronic
interface EchoResponse {
    void onEchoResponse(long time, int runIdx);
}
