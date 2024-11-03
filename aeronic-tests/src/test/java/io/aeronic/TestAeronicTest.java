package io.aeronic;

import io.aeronic.test.TestAeronic;
import org.agrona.collections.MutableLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestAeronicTest
{

    private TestAeronic aeronic;

    @BeforeEach
    void setUp()
    {
        aeronic = new TestAeronic();
    }

    @Test
    public void shouldBeAbleToPublishAndReceiveAMessage()
    {
        final TestSimpleEvents testSubscriber = new TestSimpleEvents();

        // TODO: remove abstraction leaks
        aeronic.registerSubscriber(SimpleEvents.class, testSubscriber, null, 0);
        final SimpleEvents testPublisher = aeronic.createPublisher(SimpleEvents.class, null, 0);

        assertEquals(0, testSubscriber.value.get());
        testPublisher.onEvent(101);
        assertEquals(101, testSubscriber.value.get());
    }

    @Test
    void shouldThrowIfPublisherCreatedBeforeSubscriber()
    {
        final IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> aeronic.createPublisher(SimpleEvents.class, null, 0)
        );

        assertEquals("Cannot create a publisher before registering a subscriber! " +
            "Register io.aeronic.SimpleEvents subscriber first.", exception.getMessage());
    }

    private static class TestSimpleEvents implements SimpleEvents
    {

        private final MutableLong value = new MutableLong();

        @Override
        public void onEvent(final long value)
        {
            this.value.set(value);
        }
    }
}
