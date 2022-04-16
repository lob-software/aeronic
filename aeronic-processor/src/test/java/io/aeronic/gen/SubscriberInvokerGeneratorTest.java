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
                            final long[] longArray = bufferDecoder.decodeLongArray();
                            final int[] intArray = bufferDecoder.decodeIntArray();
                            final float[] floatArray = bufferDecoder.decodeFloatArray();
                            final double[] doubleArray = bufferDecoder.decodeDoubleArray();
                            final byte[] byteArray = bufferDecoder.decodeByteArray();
                            final char[] charArray = bufferDecoder.decodeCharArray();
                            subscriber.onEvent(
                                longValue,
                                intValue,
                                floatValue,
                                doubleValue,
                                byteValue,
                                charValue,
                                simpleImpl,
                                stringValue,
                                longArray,
                                intArray,
                                floatArray,
                                doubleArray,
                                byteArray,
                                charArray
                            );
                        }
                        case 1 -> {
                            final long timestamp = bufferDecoder.decodeLong();
                            subscriber.onTimer(
                                timestamp
                            );
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
                    new ParameterInfo("longValue", "long", true, false),
                    new ParameterInfo("intValue", "int", true, false),
                    new ParameterInfo("floatValue", "float", true, false),
                    new ParameterInfo("doubleValue", "double", true, false),
                    new ParameterInfo("byteValue", "byte", true, false),
                    new ParameterInfo("charValue", "char", true, false),
                    new ParameterInfo("simpleImpl", "io.aeronic.codec.SimpleImpl", false, false),
                    new ParameterInfo("stringValue", "java.lang.String", false, false),
                    new ParameterInfo("longArray", "long[]", false, true),
                    new ParameterInfo("intArray", "int[]", false, true),
                    new ParameterInfo("floatArray", "float[]", false, true),
                    new ParameterInfo("doubleArray", "double[]", false, true),
                    new ParameterInfo("byteArray", "byte[]", false, true),
                    new ParameterInfo("charArray", "char[]", false, true)
                )),
                new MethodInfo(1, "onTimer", List.of(
                    new ParameterInfo("timestamp", "long", true, false)
                ))
            )
        );

        assertEquals(SAMPLE_SUBSCRIBER, actualSource);
    }
}
