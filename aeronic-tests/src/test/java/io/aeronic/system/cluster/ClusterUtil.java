package io.aeronic.system.cluster;

import static io.aeronic.system.cluster.TestClusterNode.LOCALHOST;

public class ClusterUtil {
    public static String clusterMembers(final int clusterId, final int memberCount)
    {
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < memberCount; i++) {
            builder
                    .append(i).append(',')
                    .append(LOCALHOST).append(":2").append(clusterId).append("11").append(i).append(',')
                    .append(LOCALHOST).append(":2").append(clusterId).append("22").append(i).append(',')
                    .append(LOCALHOST).append(":2").append(clusterId).append("33").append(i).append(',')
                    .append(LOCALHOST).append(":0,")
                    .append(LOCALHOST).append(":801").append(i).append('|');
        }

        builder.setLength(builder.length() - 1);

        return builder.toString();
    }

    public static String archiveControlRequestChannel(final int memberId)
    {
        return "aeron:udp?endpoint=localhost:801" + memberId;
    }

    public static String ingressEndpoints(final int clusterId, final int memberCount)
    {
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < memberCount; i++) {
            builder.append(i).append('=').append(LOCALHOST).append(":2").append(clusterId).append("11")
                    .append(i).append(',');
        }

        builder.setLength(builder.length() - 1);

        return builder.toString();
    }
}