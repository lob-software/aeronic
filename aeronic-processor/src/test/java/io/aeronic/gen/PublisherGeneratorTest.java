package io.aeronic.gen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PublisherGeneratorTest
{
    private static final String SAMPLE_PUBLISHER =
        """
            package io.aeronic.gen;
                    
            import io.aeron.Publication;
            import io.aeronic.net.AbstractPublisher;
            import io.aeronic.net.AeronicPublication;
            import org.agrona.BitUtil;
            import io.aeronic.codec.SimpleImpl;
            import io.aeronic.MyEnum;
            import io.aeronic.Composite;
            import java.util.List;
            
                    
            public class TestEventsPublisher extends AbstractPublisher implements TestEvents
            {
                    
                public TestEventsPublisher(final AeronicPublication publication)
                {
                    super(publication);
                }
                    
                @Override
                public void onEvent(
                    final long aLong,
                    final int intValue,
                    final float floatValue,
                    final double doubleValue,
                    final byte byteValue,
                    final char charValue,
                    final SimpleImpl simpleImpl,
                    final String stringValue,
                    final long[] longs,
                    final int[] ints,
                    final float[] floats,
                    final double[] doubles,
                    final byte[] bytes,
                    final char[] chars,
                    final SimpleImpl[] simpleImplArray,
                    final MyEnum myEnum,
                    final List<Composite> compositeList
                )
                {
                    bufferEncoder.encode(0);
                    bufferEncoder.encode(aLong);
                    bufferEncoder.encode(intValue);
                    bufferEncoder.encode(floatValue);
                    bufferEncoder.encode(doubleValue);
                    bufferEncoder.encode(byteValue);
                    bufferEncoder.encode(charValue);
                    simpleImpl.encode(bufferEncoder);
                    bufferEncoder.encode(stringValue);
                    bufferEncoder.encode(longs);
                    bufferEncoder.encode(ints);
                    bufferEncoder.encode(floats);
                    bufferEncoder.encode(doubles);
                    bufferEncoder.encode(bytes);
                    bufferEncoder.encode(chars);
                    bufferEncoder.encode(simpleImplArray);
                    myEnum.encode(bufferEncoder);
                    bufferEncoder.encode(compositeList);
                    offer();
                }
                
                @Override
                public void onTimer(
                    final long timestamp
                )
                {
                    bufferEncoder.encode(1);
                    bufferEncoder.encode(timestamp);
                    offer();
                }
            }
            """;

    @Test
    public void shouldGeneratePublisherSource()
    {
        final PublisherGenerator publisherGenerator = new PublisherGenerator();
        final String actualSource = publisherGenerator.generate(
            "io.aeronic.gen",
            "TestEvents",
            List.of(
                new MethodInfo(0, "onEvent", List.of(
                    new ParameterInfo("aLong", "long", true, false, List.of()),
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

        assertEquals(SAMPLE_PUBLISHER, actualSource);
    }
}