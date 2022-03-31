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
                final String stringValue
            )
            {
                bufferEncoder.encodeInt(0);
                bufferEncoder.encodeLong(aLong);
                bufferEncoder.encodeInt(intValue);
                bufferEncoder.encodeFloat(floatValue);
                bufferEncoder.encodeDouble(doubleValue);
                bufferEncoder.encodeByte(byteValue);
                bufferEncoder.encodeChar(charValue);
                simpleImpl.encode(bufferEncoder);
                bufferEncoder.encodeString(stringValue);
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
                    new ParameterInfo("aLong", "long", true),
                    new ParameterInfo("intValue", "int", true),
                    new ParameterInfo("floatValue", "float", true),
                    new ParameterInfo("doubleValue", "double", true),
                    new ParameterInfo("byteValue", "byte", true),
                    new ParameterInfo("charValue", "char", true),
                    new ParameterInfo("simpleImpl", "io.aeronic.codec.SimpleImpl", false),
                    new ParameterInfo("stringValue", "java.lang.String", false)
                ))
            )
        );

        assertEquals(SAMPLE_PUBLISHER, actualSource);
    }
}