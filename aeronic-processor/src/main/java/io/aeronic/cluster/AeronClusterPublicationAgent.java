package io.aeronic.cluster;

import org.agrona.concurrent.Agent;

public class AeronClusterPublicationAgent implements Agent {
    private final AeronClusterPublication publication;
    private final String publisherName;

    public AeronClusterPublicationAgent(final AeronClusterPublication publication, final String publisherName)
    {
        this.publication = publication;
        this.publisherName = publisherName;
    }

    @Override
    public int doWork()
    {
        return publication.pollCluster();
    }

    @Override
    public String roleName()
    {
        return publisherName;
    }
}
