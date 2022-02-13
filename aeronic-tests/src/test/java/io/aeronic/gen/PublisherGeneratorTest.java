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
                
        public class SampleEventsPublisher extends AbstractPublisher implements SampleEvents
        {
                
            public SampleEventsPublisher(final Publication publication)
            {
                super(publication);
            }
                
            @Override
            public void onEvent(final long longValue)
            {
                unsafeBuffer.putInt(METHOD_IDX_OFFSET, 0);
                unsafeBuffer.putLong(METHOD_IDX_OFFSET + BitUtil.SIZE_OF_INT, longValue);
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
            "SampleEvents",
            List.of(
                new MethodInfo(0, "onEvent", List.of(
                    new ParameterInfo("longValue", "long")
                ))
            )
        );

        assertEquals(SAMPLE_PUBLISHER, actualSource);
    }
}