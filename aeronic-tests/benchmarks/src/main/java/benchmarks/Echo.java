package benchmarks;

import io.aeronic.Aeronic;

@Aeronic
interface Echo
{
    void echo(long time, int runIdx);
}
