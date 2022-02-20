package io.aeronic.gen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubscriberGeneratorTest
{
    private static final String SAMPLE_SUBSCRIBER =
        """
        package io.aeronic;
                
        import io.aeronic.TestEvents;
        import io.aeron.Subscription;
        import io.aeronic.net.AbstractSubscriber;
        import io.aeronic.codec.BufferDecoder;
        import org.agrona.BitUtil;
        import org.agrona.DirectBuffer;
        import io.aeronic.SimpleImpl;
                
        public class TestEventsSubscriber extends AbstractSubscriber<TestEvents>
        {
           
            public TestEventsSubscriber(final Subscription subscription, final TestEvents subscriber)
            {
                super(subscription, subscriber);
            }
                
            public void handle(final BufferDecoder bufferDecoder, final int offset)
            {
                final int msgType = bufferDecoder.decodeInt();
                switch (msgType)
                {
                    case 0 -> {
                        final long longValue = bufferDecoder.decodeLong();
                        final int intValue = bufferDecoder.decodeInt();
                        final float floatValue = bufferDecoder.decodeFloat();
                        final double doubleValue = bufferDecoder.decodeDouble();
                        final byte byteValue = bufferDecoder.decodeByte();
                        final char charValue = bufferDecoder.decodeChar();
                        final SimpleImpl simpleImpl = SimpleImpl.decode(bufferDecoder);
                        final String stringValue = bufferDecoder.decodeString();
                        subscriber.onEvent(longValue, intValue, floatValue, doubleValue, byteValue, charValue, simpleImpl, stringValue);
                    }
                    default -> throw new RuntimeException("Unexpected message type: " + msgType);
                }
            }
                
            @Override
            public String roleName()
            {
                return "TestEvents";
            }
        }     
        """;

    @Test
    public void shouldGenerateSubscriberSource()
    {
        final SubscriberGenerator subscriberGenerator = new SubscriberGenerator();
        final String actualSource = subscriberGenerator.generate(
            "io.aeronic",
            "TestEvents",
            List.of(
                new MethodInfo(0, "onEvent", List.of(
                    new ParameterInfo("longValue", "long", true),
                    new ParameterInfo("intValue", "int", true),
                    new ParameterInfo("floatValue", "float", true),
                    new ParameterInfo("doubleValue", "double", true),
                    new ParameterInfo("byteValue", "byte", true),
                    new ParameterInfo("charValue", "char", true),
                    new ParameterInfo("simpleImpl", "io.aeronic.SimpleImpl", false),
                    new ParameterInfo("stringValue", "java.lang.String", false)
                ))
            )
        );

        assertEquals(SAMPLE_SUBSCRIBER, actualSource);
    }
}
