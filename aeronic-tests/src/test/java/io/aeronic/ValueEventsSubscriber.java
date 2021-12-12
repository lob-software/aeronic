package io.aeronic;

import io.aeron.Subscription;
import io.aeronic.net.AbstractSubscriber;
import org.agrona.BitUtil;
import org.agrona.DirectBuffer;


/**
 * To be generated. Receives message and calls user-defined topic implementation.
 */
public class ValueEventsSubscriber extends AbstractSubscriber<ValueEvents>
{

    public ValueEventsSubscriber(final Subscription subscription, final ValueEvents subscriber)
    {
        super(subscription, subscriber);
    }

    public void handle(final DirectBuffer buffer, final int offset)
    {
        final int msgType = buffer.getInt(offset);
        switch (msgType)
        {
            case 1 -> {
                // to generate
                final long value = buffer.getLong(offset + ValueEvents.BODY_OFFSET);
                System.out.println("Receiver agent called with " + value);
                subscriber.setValue(value);
            }
            case 2 -> {
                // to generate
                final long valueOne = buffer.getLong(offset + ValueEvents.BODY_OFFSET);
                final long valueTwo = buffer.getLong(offset + ValueEvents.BODY_OFFSET + BitUtil.SIZE_OF_LONG);
                System.out.println("Receiver agent called with " + valueOne + " and " + valueTwo);
                subscriber.setValues(valueOne, valueTwo);
            }
            default -> throw new RuntimeException("Unexpected message type: " + msgType);
        }
    }

    @Override
    public String roleName()
    {
        return "value-topic";
    }
}
