package io.aeronic.system.cluster;

import io.aeron.CommonContext;
import io.aeron.cluster.client.AeronCluster;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeronic.cluster.AeronicCredentialSupplier;

public class TestClusterClient
{

    public static AeronCluster connectClientToCluster(final String clientName, final String ingressChannel)
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
                .credentialsSupplier(new AeronicCredentialSupplier(clientName))
                .ingressChannel(ingressChannel)
                .errorHandler(Throwable::printStackTrace)
                .aeronDirectoryName(aeronDirectoryName));
    }
}
