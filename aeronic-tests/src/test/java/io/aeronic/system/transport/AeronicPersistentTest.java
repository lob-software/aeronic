package io.aeronic.system.transport;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import io.aeron.archive.Archive;
import io.aeron.archive.ArchiveThreadingMode;
import io.aeron.archive.client.AeronArchive;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeronic.AeronicWizard;
import io.aeronic.SampleEvents;
import org.agrona.SystemUtil;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static io.aeronic.Assertions.assertEventuallyTrue;

public class AeronicPersistentTest
{
    private static final int STREAM_ID = 1033;
    private static final String CONTROL_ENDPOINT = "localhost:23265";

    private AeronicWizard aeronic;
    private Aeron aeron;
    private MediaDriver mediaDriver;
    private Archive archive;
    private AeronArchive aeronArchive;

    private final String publicationChannel = new ChannelUriStringBuilder()
        .media(CommonContext.UDP_MEDIA)
        .controlEndpoint(CONTROL_ENDPOINT)
        .controlMode(CommonContext.MDC_CONTROL_MODE_DYNAMIC)
        .alias("publication")
        .build();

    @BeforeEach
    void setUp()
    {
        mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
            .dirDeleteOnStart(true)
            .spiesSimulateConnection(true)
            .threadingMode(ThreadingMode.SHARED)
            .sharedIdleStrategy(new BusySpinIdleStrategy())
            .dirDeleteOnShutdown(true));

        archive = Archive.launch(new Archive.Context()
            .archiveDir(new File(SystemUtil.tmpDirName(), "archive"))
            .aeronDirectoryName(mediaDriver.context().aeronDirectoryName())
            .errorHandler(Throwable::printStackTrace)
            .deleteArchiveOnStart(true)
            .threadingMode(ArchiveThreadingMode.SHARED)
            .controlChannel("aeron:udp?endpoint=localhost:8010")
            .replicationChannel("aeron:udp?endpoint=localhost:0"));

        aeron = Aeron.connect(new Aeron.Context().aeronDirectoryName(mediaDriver.aeronDirectoryName()));

        aeronArchive = AeronArchive.connect(new AeronArchive.Context()
            .errorHandler(Throwable::printStackTrace)
            .aeron(aeron)
            .controlRequestChannel(archive.context().localControlChannel())
            .controlRequestStreamId(archive.context().localControlStreamId())
            .controlResponseChannel(archive.context().controlChannel()));

        aeronic = AeronicWizard.launch(
            new AeronicWizard.Context()
                .aeron(aeron)
                .aeronArchive(aeronArchive)
                .idleStrategy(NoOpIdleStrategy.INSTANCE)
                .errorHandler(Throwable::printStackTrace)
                .atomicCounter(null));
    }

    @Test
    public void subscriberCanReplayMerge()
    {
        final SampleEvents publisher = aeronic.createPersistentPublisher(SampleEvents.class, publicationChannel, STREAM_ID);
        final SampleEventsImpl subscriberImpl = new SampleEventsImpl();
        aeronic.registerSubscriber(SampleEvents.class, subscriberImpl, publicationChannel, STREAM_ID);

        publisher.onEvent(101L);
        publisher.onEvent(102L);

        assertEventuallyTrue(() -> subscriberImpl.value == 102L);

        final SampleEventsImpl persistentSampleEvents = new SampleEventsImpl();

        aeronic.registerPersistentSubscriber(SampleEvents.class, persistentSampleEvents, "publication", STREAM_ID);

        assertEventuallyTrue(() -> persistentSampleEvents.count == 2);

        publisher.onEvent(103L);

        assertEventuallyTrue(() -> persistentSampleEvents.count == 3 && persistentSampleEvents.value == 103L);
    }

    private static class SampleEventsImpl implements SampleEvents
    {

        private volatile long value;
        private int count;

        @Override
        public void onEvent(final long value)
        {
            count++;
            this.value = value;
        }
    }

    @AfterEach
    void tearDown()
    {
        aeronic.close();
        aeronArchive.close();
        archive.close();
        aeron.close();
        mediaDriver.close();
    }
}
