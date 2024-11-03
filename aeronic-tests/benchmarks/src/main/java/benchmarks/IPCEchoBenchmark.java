package benchmarks;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeronic.AeronicImpl;
import one.profiler.AsyncProfiler;
import one.profiler.Events;
import org.HdrHistogram.Histogram;
import org.agrona.concurrent.NoOpIdleStrategy;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static io.aeron.CommonContext.IPC_CHANNEL;

// TODO: account for coordinated omission
public class IPCEchoBenchmark
{
    private static final int RUNS = 10_000_000;

    private AeronicImpl aeronic;
    private Echo echoProxy;
    private MediaDriver mediaDriver;
    private Aeron aeron;
    private EchoResponseListener echoResponseListener;

    private void createAeronic()
    {
        final MediaDriver.Context mediaDriverCtx = new MediaDriver.Context()
            .dirDeleteOnStart(true)
            .dirDeleteOnShutdown(true)
            .ipcPublicationTermWindowLength(1024 * 1024)
            .threadingMode(ThreadingMode.DEDICATED)
            .conductorIdleStrategy(NoOpIdleStrategy.INSTANCE)
            .senderIdleStrategy(NoOpIdleStrategy.INSTANCE)
            .receiverIdleStrategy(NoOpIdleStrategy.INSTANCE);

        mediaDriver = MediaDriver.launchEmbedded(mediaDriverCtx);

        final Aeron.Context aeronCtx = new Aeron.Context()
            .idleStrategy(NoOpIdleStrategy.INSTANCE)
            .awaitingIdleStrategy(NoOpIdleStrategy.INSTANCE)
            .aeronDirectoryName(mediaDriver.aeronDirectoryName());

        aeron = Aeron.connect(aeronCtx);

        aeronic = AeronicImpl.launch(
            new AeronicImpl.Context()
                .aeron(aeron)
                .offerFailureHandler(l -> LockSupport.parkNanos(1))
                .idleStrategy(NoOpIdleStrategy.INSTANCE)
                .errorHandler(Throwable::printStackTrace)
                .atomicCounter(null));
    }

    private void setUp()
    {
        createAeronic();

        final EchoResponse echoResponseProxy = aeronic.createPublisher(EchoResponse.class, IPC_CHANNEL, 102);
        echoResponseListener = new EchoResponseListener();
        aeronic.registerSubscriber(EchoResponse.class, echoResponseListener, IPC_CHANNEL, 102);

        echoProxy = aeronic.createPublisher(Echo.class, IPC_CHANNEL, 101);
        final EchoService echoService = new EchoService(echoResponseProxy);
        aeronic.registerSubscriber(Echo.class, echoService, IPC_CHANNEL, 101);

        aeronic.awaitUntilPubsAndSubsConnect();
    }

    private static final class EchoResponseListener implements EchoResponse
    {
        private final Histogram histogram = new Histogram(Long.MAX_VALUE, 5);
        private volatile boolean completed = false;
        private int responsesReceived = 0;

        @Override
        public void onEchoResponse(final long time, final int runIdx)
        {
            // receive time being read from different thread than the send time
            final long receiveTime = System.nanoTime();
            histogram.recordValue(receiveTime - time);

            if (++responsesReceived == RUNS)
            {
                completed = true;
            }
        }

        public void reset()
        {
            responsesReceived = 0;
            completed = false;
        }
    }

    private void run()
    {
        for (int i = 0; i < RUNS; i++)
        {
            final long sendTime = System.nanoTime();
            echoProxy.echo(sendTime, i);
        }
    }

    private void awaitCompletion()
    {
        while (!echoResponseListener.completed)
        {
            LockSupport.parkNanos(100);
        }
    }

    private void report()
    {
        final Histogram histogram = echoResponseListener.histogram;
        System.out.println("Min (μs): " + TimeUnit.NANOSECONDS.toMicros(histogram.getMinValue()));
        System.out.println("Median (μs): " + TimeUnit.NANOSECONDS.toMicros(histogram.getValueAtPercentile(50)));
        System.out.println("Max (μs): " + TimeUnit.NANOSECONDS.toMicros(histogram.getMaxValue()));

        try
        {
            histogram.outputPercentileDistribution(
                new PrintStream(
                    new FileOutputStream(System.getProperty("histogram-file-name", "histogram.hgrm"))), 1000.);
        }
        catch (final FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void tearDown()
    {
        aeronic.close();
        aeron.close();
        mediaDriver.close();
    }

    private void reset()
    {
        echoResponseListener.reset();
    }

    public static void main(final String[] args) throws IOException
    {
        final AsyncProfiler profiler = AsyncProfiler.getInstance();

        final IPCEchoBenchmark benchmark = new IPCEchoBenchmark();

        benchmark.setUp();

        System.out.println("Warming up...");

        for (int i = 0; i < 5; i++)
        {
            // warm up
            benchmark.run();
            benchmark.awaitCompletion();
            benchmark.reset();
        }

        System.out.println("Warm up complete, running the benchmark...");

        // run
        profiler.start(Events.ALLOC, 64);

        benchmark.run();
        benchmark.awaitCompletion();

        profiler.stop();

        benchmark.report();

        profiler.execute("dump,file=output.html");

        benchmark.tearDown();
    }
}
