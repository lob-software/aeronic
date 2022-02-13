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
        import org.agrona.BitUtil;
                
        import static io.aeronic.net.Constants.METHOD_IDX_OFFSET;
                
        public class TestEventsPublisher extends AbstractPublisher implements TestEvents
        {
                
            public TestEventsPublisher(final Publication publication)
            {
                super(publication);
            }
                
            @Override
            public void onEvent(final long aLong, final int intValue, final float floatValue, final double doubleValue, final byte byteValue, final char charValue)
            {
                bufferEncoder.encodeInt(0);
                bufferEncoder.encodeLong(aLong);
                bufferEncoder.encodeInt(intValue);
                bufferEncoder.encodeFloat(floatValue);
                bufferEncoder.encodeDouble(doubleValue);
                bufferEncoder.encodeByte(byteValue);
                bufferEncoder.encodeChar(charValue);
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
                    new ParameterInfo("aLong", "long"),
                    new ParameterInfo("intValue", "int"),
                    new ParameterInfo("floatValue", "float"),
                    new ParameterInfo("doubleValue", "double"),
                    new ParameterInfo("byteValue", "byte"),
                    new ParameterInfo("charValue", "char")
                ))
            )
        );

        assertEquals(SAMPLE_PUBLISHER, actualSource);
    }
}