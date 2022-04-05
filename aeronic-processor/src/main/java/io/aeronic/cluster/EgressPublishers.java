package io.aeronic.cluster;

import java.util.function.Consumer;

public class EgressPublishers
{
    private final ClientSessionPublication<?>[] clientSessionPublications;

    private EgressPublishers()
    {
        this.clientSessionPublications = new ClientSessionPublication[0];
    }

    private EgressPublishers(final ClientSessionPublication<?>... clientSessionPublications)
    {
        this.clientSessionPublications = clientSessionPublications;
    }

    public static EgressPublishers none()
    {
        return new EgressPublishers();
    }

    public static EgressPublishers create(final ClientSessionPublication<?>... clientSessionPublications)
    {
        return new EgressPublishers(clientSessionPublications);
    }

    public void forEach(final Consumer<ClientSessionPublication<?>> clientSessionPublicationConsumer)
    {
        for (final ClientSessionPublication<?> clientSessionPublication : clientSessionPublications)
        {
            clientSessionPublicationConsumer.accept(clientSessionPublication);
        }
    }
}
