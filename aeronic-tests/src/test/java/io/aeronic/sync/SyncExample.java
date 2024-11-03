package io.aeronic.sync;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeronic.AeronicImpl;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SyncExample
{
    private static final String CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .endpoint("localhost:40457")
        .build();

    private AeronicImpl aeronic;
    private Aeron aeron;
    private MediaDriver mediaDriver;

    @BeforeEach
    void setUp()
    {
        final MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
            .dirDeleteOnStart(true)
            .spiesSimulateConnection(true)
            .threadingMode(ThreadingMode.SHARED)
            .sharedIdleStrategy(new BusySpinIdleStrategy())
            .dirDeleteOnShutdown(true);

        mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);

        final Aeron.Context aeronCtx = new Aeron.Context()
            .aeronDirectoryName(mediaDriver.aeronDirectoryName());

        aeron = Aeron.connect(aeronCtx);
        aeronic = AeronicImpl.launch(new AeronicImpl.Context().aeron(aeron));
    }

    @AfterEach
    void tearDown()
    {
        aeronic.close();
        aeron.close();
        mediaDriver.close();
    }

    @Test
    public void sync()
    {
        final SyncEvents syncEventsPublisher = aeronic.createPublisher(SyncEvents.class, CHANNEL, 15);
        final SyncEventsController syncEventsController = new SyncEventsController(syncEventsPublisher);
        final SyncEventsResponse syncEventsResponsePublisher = aeronic.createPublisher(SyncEventsResponse.class, CHANNEL, 16);
        final SyncEventsImpl syncEventsSubscriber = new SyncEventsImpl(syncEventsResponsePublisher);

        aeronic.registerSubscriber(SyncEvents.class, syncEventsSubscriber, CHANNEL, 15);
        aeronic.registerSubscriber(SyncEventsResponse.class, syncEventsController, CHANNEL, 16);
        aeronic.awaitUntilPubsAndSubsConnect();

        final long expectedValue = 645;
        final long actualValue = syncEventsController.addAndGet(expectedValue).join();
        assertEquals(expectedValue, actualValue);

        final long newValue = syncEventsController.addAndGet(101).join();
        assertEquals(746, newValue);
    }

    private static class SyncEventsImpl implements SyncEvents
    {

        private final SyncEventsResponse responsePublisher;
        private long value = 0L;

        public SyncEventsImpl(final SyncEventsResponse responsePublisher)
        {
            this.responsePublisher = responsePublisher;
        }

        @Override
        public void addValue(final long correlationId, final long value)
        {
            // 2. async publish the response or ack that value has been received
            this.value += value;
            this.responsePublisher.onEventResponse(correlationId, this.value);
        }
    }

    private static class SyncEventsController implements SyncEventsResponse
    {
        final LongSupplier correlationIdSupplier = System::nanoTime;
        private final SyncEvents publisher;
        private final Map<Long, CompletableFuture<Long>> correlationIdToResponseMap = new ConcurrentHashMap<>();

        public SyncEventsController(final SyncEvents publisher)
        {
            this.publisher = publisher;
        }

        @Override
        public void onEventResponse(final long correlationId, final long value)
        {
            // 3. receive the response and update local data structure
            // NOTE: there is a potential for memory leak if response channel does not send the response back
            // a timeout can be set so that upon expiry, correlationId is removed from the map
            correlationIdToResponseMap.remove(correlationId).complete(value);
        }

        public CompletableFuture<Long> addAndGet(final long value)
        {
            // 1. async publish the value
            final long correlationId = correlationIdSupplier.getAsLong();
            final CompletableFuture<Long> future = new CompletableFuture<>();
            correlationIdToResponseMap.put(correlationId, future);
            publisher.addValue(correlationId, value);

            // 4. wait for same-correlationId-response to be received on the current thread / return future so caller can decide
            return future;
        }
    }
}
