package io.aeronic.system.multiparam;

import io.aeronic.MyEnum;
import io.aeronic.gen.Aeronic;

import java.util.List;

@Aeronic
public interface MultiParamEvents
{
    void onEvent(
            long longValue,
            int intValue,
            float floatValue,
            double doubleValue,
            byte byteValue,
            char charValue,
            boolean booleanValue,
            short shortValue,
            String stringValue,
            Composite compositeValue,
            long[] longs,
            int[] ints,
            double[] doubles,
            float[] floats,
            short[] shorts,
            byte[] bytes,
            char[] chars,
            Composite[] compositeArray,
            MyEnum myEnum,
            List<Composite> compositeList
                );
}
