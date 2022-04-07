package io.aeronic.cluster;

import io.aeronic.AeronicWizard;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EgressPublishers
{
    private final List<ClientSessionPublication<?>> clientSessionPublications = new ArrayList<>();

    public void forEach(final Consumer<ClientSessionPublication<?>> clientSessionPublicationConsumer)
    {
        for (final ClientSessionPublication<?> clientSessionPublication : clientSessionPublications)
        {
            clientSessionPublicationConsumer.accept(clientSessionPublication);
        }
    }

    public <T> void register(final Class<T> clazz)
    {
        clientSessionPublications.add(AeronicWizard.createClusterEgressPublisher(clazz));
    }
}
