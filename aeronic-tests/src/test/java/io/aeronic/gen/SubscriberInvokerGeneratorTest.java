package io.aeronic.gen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubscriberInvokerGeneratorTest
{
    private static final String SAMPLE_SUBSCRIBER =
        """
            package io.aeronic;
                    
            import io.aeronic.TestEvents;
            import io.aeron.Subscription;
            import io.aeronic.net.AbstractSubscriberInvoker;
            import io.aeronic.codec.BufferDecoder;
            import org.agrona.BitUtil;
            import org.agrona.DirectBuffer;
            import io.aeronic.codec.SimpleImpl;
                    
            public class TestEventsInvoker extends AbstractSubscriberInvoker<TestEvents>
            {
               
                public TestEventsInvoker(final TestEvents subscriber)
                {
                    super(subscriber);
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
                        case 1 -> {
                            final long timestamp = bufferDecoder.decodeLong();
                            subscriber.onTimer(timestamp);
                        }
                    }
                }
            }     
            """;

    @Test
    public void shouldGenerateSubscriberSource()
    {
        final SubscriberInvokerGenerator subscriberInvokerGenerator = new SubscriberInvokerGenerator();
        final String actualSource = subscriberInvokerGenerator.generate(
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
                    new ParameterInfo("simpleImpl", "io.aeronic.codec.SimpleImpl", false),
                    new ParameterInfo("stringValue", "java.lang.String", false)
                )),
                new MethodInfo(1, "onTimer", List.of(
                    new ParameterInfo("timestamp", "long", true)
                ))
            )
        );

        assertEquals(SAMPLE_SUBSCRIBER, actualSource);
    }
}
