package io.aeronic;

import org.agrona.BitUtil;


public interface ValueEvents
{
    int MESSAGE_TYPE_OFFSET = 0;
    int BODY_OFFSET = MESSAGE_TYPE_OFFSET + BitUtil.SIZE_OF_INT;

    void setValue(long value);

    void setValues(long valueOne, long valueTwo);
}
