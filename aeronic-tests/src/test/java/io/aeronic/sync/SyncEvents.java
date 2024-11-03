package io.aeronic.sync;

import io.aeronic.gen.Aeronic;

@Aeronic
public interface SyncEvents
{
    void addValue(long correlationId, long value);
}
