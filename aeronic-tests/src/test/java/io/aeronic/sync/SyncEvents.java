package io.aeronic.sync;

import io.aeronic.Aeronic;

@Aeronic
public interface SyncEvents
{
    void addValue(long correlationId, long value);
}
