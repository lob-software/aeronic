package io.aeronic.system.multiparam;

import io.aeronic.Aeronic;

@Aeronic
public interface MultiParamEvents
{
    void onEvent(long longValue, int intValue, float floatValue, double doubleValue, byte byteValue, char charValue, boolean booleanValue, short shortValue);
}
