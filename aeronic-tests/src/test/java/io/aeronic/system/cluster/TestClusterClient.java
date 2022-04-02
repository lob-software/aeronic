package io.aeronic.system.cluster;

import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.cluster.client.EgressListener;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.logbuffer.Header;
import io.aeron.security.CredentialsSupplier;
import org.agrona.DirectBuffer;
import org.agrona.collections.ArrayUtil;

public class TestClusterClient
{

    private static final String INGRESS_CHANNEL = new ChannelUriStringBuilder()
        .media("udp")
        .reliable(true)
        .endpoint("localhost:40457")
        .build();

    private final String clientName;

    public TestClusterClient(final String clientName)
    {
        this.clientName = clientName;
    }

    private final EgressListener egressMessageListener = new EgressListener()
    {
        public void onMessage(
            final long clusterSessionId,
            final long timestamp,
            final DirectBuffer buffer,
            final int offset,
            final int length,
            final Header header
        )
        {
            System.out.println("egress onMessage " + clusterSessionId);
        }

        public void onNewLeader(
            final long clusterSessionId,
            final long leadershipTermId,
            final int leaderMemberId,
            final String ingressEndpoints
        )
        {
            System.out.println("TestClusterNode.onNewLeader");
        }
    };

    public AeronCluster connectClientToCluster()
    {
        final String aeronDirectoryName = CommonContext.getAeronDirectoryName() + "-" + clientName;

        MediaDriver.launch(
            new MediaDriver.Context()
                .aeronDirectoryName(aeronDirectoryName)
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true)
                .errorHandler(Throwable::printStackTrace));

        return AeronCluster.connect(
            new AeronCluster.Context()
                .credentialsSupplier(new SimpleCredentialsSupplier(clientName))
                .ingressChannel(INGRESS_CHANNEL)
                .errorHandler(Throwable::printStackTrace)
                .egressListener(egressMessageListener)
                .aeronDirectoryName(aeronDirectoryName));
    }

    private static class SimpleCredentialsSupplier implements CredentialsSupplier
    {
        private final String name;

        public SimpleCredentialsSupplier(String name)
        {
            this.name = name;
        }

        @Override
        public byte[] encodedCredentials()
        {
            return name.getBytes();
        }

        @Override
        public byte[] onChallenge(byte[] encodedChallenge)
        {
            return ArrayUtil.EMPTY_BYTE_ARRAY;
        }
    }
}
