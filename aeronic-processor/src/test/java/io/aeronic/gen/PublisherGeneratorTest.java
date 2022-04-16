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
                    final long[] longArray,
                    final int[] intArray,
                    final float[] floatArray,
                    final double[] doubleArray,
                    final byte[] byteArray,
                    final char[] charArray
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
                    bufferEncoder.encode(longArray);
                    bufferEncoder.encode(intArray);
                    bufferEncoder.encode(floatArray);
                    bufferEncoder.encode(doubleArray);
                    bufferEncoder.encode(byteArray);
                    bufferEncoder.encode(charArray);
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
                    new ParameterInfo("aLong", "long", true, false),
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

        assertEquals(SAMPLE_PUBLISHER, actualSource);
    }
}