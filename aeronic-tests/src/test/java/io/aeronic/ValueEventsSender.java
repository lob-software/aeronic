package io.aeronic;

import io.aeron.Publication;
import org.agrona.BitUtil;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

/**
 * Implementation to invoked by publishers of the topic. Should be able to generate this class given a Topic interface
 */
public class ValueEventsSender implements ValueEvents
{
    private final Publication publication;
    private final UnsafeBuffer unsafeBuffer;

    public ValueEventsSender(final Publication publication)
    {
        this.publication = publication;
        this.unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(64));
    }

    @Override
    public void setValue(final long value)
    {
        // marshalling will need to be generated
        unsafeBuffer.putInt(MESSAGE_TYPE_OFFSET, 1);
        unsafeBuffer.putLong(BODY_OFFSET, value);
        offer();
    }

    @Override
    public void setValues(final long valueOne, final long valueTwo)
    {
        // marshalling will need to be generated
        unsafeBuffer.putInt(MESSAGE_TYPE_OFFSET, 2);
        unsafeBuffer.putLong(BODY_OFFSET, valueOne);
        unsafeBuffer.putLong(BODY_OFFSET + BitUtil.SIZE_OF_LONG, valueTwo);
        offer();
    }

    private void offer()
    {
        if (publication.isConnected())
        {
            publication.offer(unsafeBuffer);
        }
    }
}
