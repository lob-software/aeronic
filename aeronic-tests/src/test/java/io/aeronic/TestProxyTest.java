package io.aeronic;

import org.agrona.concurrent.NoOpIdleStrategy;
import org.junit.jupiter.api.Test;

public class TestProxyTest
{

    @Test
    public void test()
    {
        final Aeronic aeronic = Aeronic.launch(
            new Aeronic.Context()
                .idleStrategy(NoOpIdleStrategy.INSTANCE)
                .errorHandler(Throwable::printStackTrace)
                .atomicCounter(null));

        // API spike:
        // method signatures should be the same between real / test implementations so that aeronic instances can
        // be injected into test / prod code
        aeronic.createTestPublisher(SimpleEvents.class);
        aeronic.registerTestSubscriber(SimpleEvents.class, new SimpleEvents() {

            @Override
            public void onEvent(final long value)
            {
                throw new RuntimeException("boom");
            }
        });
    }
}
