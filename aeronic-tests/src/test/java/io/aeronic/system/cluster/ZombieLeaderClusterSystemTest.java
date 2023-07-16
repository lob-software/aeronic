package io.aeronic.system.cluster;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.cluster.service.Cluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeronic.AeronicWizard;
import io.aeronic.SimpleEvents;
import io.aeronic.cluster.AeronicClusteredServiceContainer;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static io.aeronic.Assertions.assertEventuallyTrue;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class ZombieLeaderClusterSystemTest
{
    private static final int STREAM_ID = 101;
    private static final String UDP_MULTICAST_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .endpoint("224.0.1.1:40457")
        .networkInterface("localhost")
        .build();

    private AeronicWizard aeronic;
    private Aeron aeron;
    private MediaDriver mediaDriver;
    private final TestCluster testCluster = new TestCluster();

    @BeforeEach
    void setUp()
    {
        mediaDriver = MediaDriver.launchEmbedded(new MediaDriver.Context()
            .dirDeleteOnStart(true)
            .dirDeleteOnShutdown(true)
            .spiesSimulateConnection(true)
            .threadingMode(ThreadingMode.SHARED)
            .sharedIdleStrategy(new BusySpinIdleStrategy()));

        final Aeron.Context aeronCtx = new Aeron.Context()
            .aeronDirectoryName(mediaDriver.aeronDirectoryName());

        aeron = Aeron.connect(aeronCtx);
        aeronic = AeronicWizard.launch(new AeronicWizard.Context().aeron(aeron));

        testCluster.registerNode(0, 3, newClusteredServiceContainer());
        testCluster.registerNode(1, 3, newClusteredServiceContainer());
        testCluster.registerNode(2, 3, newClusteredServiceContainer());
    }

    private AeronicClusteredServiceContainer newClusteredServiceContainer()
    {
        return new AeronicClusteredServiceContainer(
            new AeronicClusteredServiceContainer.Configuration()
                .clusteredService(new TestClusterNode.Service())
                .registerToggledEgressPublisher(SimpleEvents.class, UDP_MULTICAST_CHANNEL, STREAM_ID)
        );
    }

    @AfterEach
    void tearDown()
    {
        aeronic.close();
        aeron.close();
        mediaDriver.close();
        testCluster.close();
    }

    @Test
    public void zombieLeaderCannotPublishAfterItIsRecovered()
    {
        final SimpleEventsImpl sub1 = new SimpleEventsImpl();
        final SimpleEventsImpl sub2 = new SimpleEventsImpl();

        // register as normal (non-cluster) aeron subs
        aeronic.registerSubscriber(SimpleEvents.class, sub1, UDP_MULTICAST_CHANNEL, STREAM_ID);
        aeronic.registerSubscriber(SimpleEvents.class, sub2, UDP_MULTICAST_CHANNEL, STREAM_ID);

        final AeronicClusteredServiceContainer leader = testCluster.waitForLeader();
        final SimpleEvents leaderPublisher = leader.getToggledPublisherFor(SimpleEvents.class);
        final int leaderIdx = testCluster.getNodeIdx(leader);
        assertEventuallyTrue(leader::egressConnected);

        // put leader to sleep and wait for him to wake up and turn follower
        ConsensusWorkIntercept.shouldStall = true;
        assertEventuallyTrue(() -> !ConsensusWorkIntercept.shouldStall && leader.getRole() == Cluster.Role.FOLLOWER, 10_000);

        final AeronicClusteredServiceContainer newLeader = testCluster.waitForLeader(leaderIdx, 10_000);
        final SimpleEvents newLeaderPublisher = newLeader.getToggledPublisherFor(SimpleEvents.class);
        assertEventuallyTrue(newLeader::egressConnected);

        newLeaderPublisher.onEvent(101L);
        // publishing from old leader should have no effect, as it is now a follower
        leaderPublisher.onEvent(202L);

        assertEventuallyTrue(() -> sub1.value == 101L && sub2.value == 101L, 5000);
    }

    public static class SimpleEventsImpl implements SimpleEvents
    {

        private volatile long value;

        @Override
        public void onEvent(final long value)
        {
            this.value = value;
        }
    }

    @BeforeAll
    static void instrumentLeaderSleep()
    {
        try
        {
            ByteBuddyAgent.install();

            final Class<?> consensusModuleAgentClazz = Class.forName("io.aeron.cluster.ConsensusModuleAgent");

            new ByteBuddy()
                .redefine(consensusModuleAgentClazz)
                .visit(Advice.to(ConsensusWorkIntercept.class).on(named("consensusWork")))
                .make()
                .load(consensusModuleAgentClazz.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent())
                .getLoaded();
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static class ConsensusWorkIntercept
    {
        public static volatile boolean shouldStall = false;

        @Advice.OnMethodEnter
        public static void consensusWork(final long nowNs, @Advice.This Object consensusModuleAgent)
        {
            try
            {
                final Field field = consensusModuleAgent.getClass().getDeclaredField("role");
                field.setAccessible(true);
                final Cluster.Role clusterRole = (Cluster.Role)field.get(consensusModuleAgent);

                if (shouldStall && clusterRole == Cluster.Role.LEADER)
                {
                    shouldStall = false;
                    LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));
                }
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
