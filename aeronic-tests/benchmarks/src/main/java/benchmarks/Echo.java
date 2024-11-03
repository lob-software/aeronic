package benchmarks;

import io.aeronic.gen.Aeronic;

@Aeronic
interface Echo
{
    void echo(long time, int runIdx);
}
