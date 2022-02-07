package io.aeronic;

import io.aeronic.gen.Aeronic;

@Aeronic
public interface SimpleEvents
{
    void onEvent(long value);
}
