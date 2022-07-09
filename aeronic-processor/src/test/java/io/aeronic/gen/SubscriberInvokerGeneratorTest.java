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
            import io.aeronic.MyEnum;
            import io.aeronic.Composite;
            import java.util.List;
            import java.util.ArrayList;
                        
                    
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
                            final long[] longs = bufferDecoder.decodeLongArray();
                            final int[] ints = bufferDecoder.decodeIntArray();
                            final float[] floats = bufferDecoder.decodeFloatArray();
                            final double[] doubles = bufferDecoder.decodeDoubleArray();
                            final byte[] bytes = bufferDecoder.decodeByteArray();
                            final char[] chars = bufferDecoder.decodeCharArray();
                            final SimpleImpl[] simpleImplArray = bufferDecoder.decodeArray(SimpleImpl::decode, SimpleImpl[]::new);
                            final MyEnum myEnum = MyEnum.decode(bufferDecoder);
                            final List<Composite> compositeList = bufferDecoder.decodeList(Composite::decode, ArrayList::new);
                            subscriber.onEvent(
                                longValue,
                                intValue,
                                floatValue,
                                doubleValue,
                                byteValue,
                                charValue,
                                simpleImpl,
                                stringValue,
                                longs,
                                ints,
                                floats,
                                doubles,
                                bytes,
                                chars,
                                simpleImplArray,
                                myEnum,
                                compositeList
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
                    new ParameterInfo("longValue", "long", true, false, List.of()),
                    new ParameterInfo("intValue", "int", true, false, List.of()),
                    new ParameterInfo("floatValue", "float", true, false, List.of()),
                    new ParameterInfo("doubleValue", "double", true, false, List.of()),
                    new ParameterInfo("byteValue", "byte", true, false, List.of()),
                    new ParameterInfo("charValue", "char", true, false, List.of()),
                    new ParameterInfo("simpleImpl", "io.aeronic.codec.SimpleImpl", false, false, List.of()),
                    new ParameterInfo("stringValue", "java.lang.String", false, false, List.of()),
                    new ParameterInfo("longs", "long[]", false, true, List.of()),
                    new ParameterInfo("ints", "int[]", false, true, List.of()),
                    new ParameterInfo("floats", "float[]", false, true, List.of()),
                    new ParameterInfo("doubles", "double[]", false, true, List.of()),
                    new ParameterInfo("bytes", "byte[]", false, true, List.of()),
                    new ParameterInfo("chars", "char[]", false, true, List.of()),
                    new ParameterInfo("simpleImplArray", "io.aeronic.codec.SimpleImpl[]", false, true, List.of()),
                    new ParameterInfo("myEnum", "io.aeronic.MyEnum", false, false, List.of()),
                    new ParameterInfo(
                        "compositeList",
                        "java.util.List<io.aeronic.Composite>",
                        false,
                        false,
                        List.of("io.aeronic.Composite")
                    )
                )),
                new MethodInfo(1, "onTimer", List.of(
                    new ParameterInfo("timestamp", "long", true, false, List.of())
                ))
            )
        );

        assertEquals(SAMPLE_SUBSCRIBER, actualSource);
    }
}
