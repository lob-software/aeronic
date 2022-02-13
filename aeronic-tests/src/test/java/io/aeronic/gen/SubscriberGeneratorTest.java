package io.aeronic.gen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubscriberGeneratorTest
{
    private static final String SAMPLE_SUBSCRIBER =
        """
        package io.aeronic;
                
        import io.aeronic.SampleEvents;
        import io.aeron.Subscription;
        import io.aeronic.net.AbstractSubscriber;
        import org.agrona.BitUtil;
        import org.agrona.DirectBuffer;
                
        public class SampleEventsSubscriber extends AbstractSubscriber<SampleEvents>
        {
           
            public SampleEventsSubscriber(final Subscription subscription, final SampleEvents subscriber)
            {
                super(subscription, subscriber);
            }
                
            public void handle(final DirectBuffer buffer, final int offset)
            {
                final int msgType = buffer.getInt(offset);
                switch (msgType)
                {
                    case 0 -> {
                        final long longValue = buffer.getLong(offset + BitUtil.SIZE_OF_INT);
                        subscriber.onEvent(longValue);
                    }
                    default -> throw new RuntimeException("Unexpected message type: " + msgType);
                }
            }
                
            @Override
            public String roleName()
            {
                return "SampleEvents";
            }
        }     
        """;

    @Test
    public void shouldGenerateSubscriberSource()
    {
        final SubscriberGenerator subscriberGenerator = new SubscriberGenerator();
        final String actualSource = subscriberGenerator.generate(
            "io.aeronic",
            "SampleEvents",
            List.of(
                new MethodInfo(0, "onEvent", List.of(
                    new ParameterInfo("longValue", "long")
                ))
            )
        );

        assertEquals(SAMPLE_SUBSCRIBER, actualSource);
    }
}
