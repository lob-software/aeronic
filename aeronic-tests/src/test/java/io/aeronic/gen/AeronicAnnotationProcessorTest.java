package io.aeronic.gen;

import org.junit.jupiter.api.Test;

public class AeronicAnnotationProcessorTest {

    @Test
    public void shouldGenerateSubscriber() throws Exception
    {
        Class.forName("io.aeronic.SampleEventsInvoker");
    }

    @Test
    public void shouldGeneratePublisher() throws Exception
    {
        Class.forName("io.aeronic.SampleEventsPublisher");
    }
}