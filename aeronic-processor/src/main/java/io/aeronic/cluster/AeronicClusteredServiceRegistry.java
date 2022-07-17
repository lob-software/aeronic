package io.aeronic.cluster;

import io.aeron.Aeron;
import io.aeron.cluster.service.ClientSession;
import io.aeron.cluster.service.Cluster;
import io.aeronic.AeronicWizard;
import io.aeronic.net.AbstractSubscriberInvoker;
import io.aeronic.net.NullSubscriberInvokerImpl;
import io.aeronic.net.ToggledAeronicPublication;
import org.agrona.DirectBuffer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AeronicClusteredServiceRegistry
{
    private final Map<Long, AbstractSubscriberInvoker<?>> invokersBySessionId = new HashMap<>();
    private final Map<String, AbstractSubscriberInvoker<?>> invokerByName = new HashMap<>();

    private final Map<String, ClientSessionPublication<?>> clientSessionPublicationByName = new HashMap<>();
    private final Map<String, ToggledAeronicPublication<?>> multiplexPublicationByName = new HashMap<>();

    public void registerEgressPublisher(final ClientSessionPublication<?> clientSessionPublication)
    {
        clientSessionPublicationByName.put(clientSessionPublication.getName(), clientSessionPublication);
    }

    public <T> void registerToggledEgressPublisher(
        final Supplier<Aeron> aeron,
        final Class<T> clazz,
        final String egressChannel,
        final int streamId
    )
    {
        final String publisherName = clazz.getName() + "__EgressPublisher";
        final ToggledAeronicPublication<T> publication = new ToggledAeronicPublication<>(() -> aeron.get().addPublication(egressChannel, streamId));
        final T publisher = AeronicWizard.createPublisher(clazz, publication);
        publication.bindPublisher(publisher);
        multiplexPublicationByName.put(publisherName, publication);
    }

    public void onRoleChange(final Cluster.Role newRole)
    {
        if (newRole == Cluster.Role.LEADER)
        {
            multiplexPublicationByName.values().forEach(ToggledAeronicPublication::activate);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getPublisherFor(final Class<T> clazz)
    {
        try
        {
            return (T)clientSessionPublicationByName.get(clazz.getName() + "__EgressPublisher").getPublisher();
        }
        catch (final Exception e)
        {
            throw new RuntimeException("Could not retrieve publisher for " + clazz);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getMultiplexingPublisherFor(final Class<T> clazz)
    {
        try
        {
            return (T)multiplexPublicationByName.get(clazz.getName() + "__EgressPublisher").getPublisher();
        }
        catch (final Exception e)
        {
            throw new RuntimeException("Could not retrieve publisher for " + clazz);
        }
    }

    public void registerIngressSubscriberInvoker(final AbstractSubscriberInvoker<?> subscriberInvoker)
    {
        Arrays.stream(subscriberInvoker.getSubscriber().getClass().getInterfaces())
            .map(Class::getCanonicalName)
            .forEach(subscriberInterface -> invokerByName.put(subscriberInterface + "__IngressPublisher", subscriberInvoker));
    }

    public boolean egressConnected()
    {
        return clientSessionPublicationByName.values().stream().allMatch(ClientSessionPublication::isConnected)
            && multiplexPublicationByName.values().stream().allMatch(ToggledAeronicPublication::isConnected);
    }

    public void close()
    {
        clientSessionPublicationByName.values().forEach(ClientSessionPublication::close);
        multiplexPublicationByName.values().forEach(ToggledAeronicPublication::close);
    }

    public void onSessionOpen(final ClientSession session)
    {
        final byte[] encodedPrincipal = session.encodedPrincipal();
        final String subscriberName = new String(encodedPrincipal);

        if (encodedPrincipal.length != 0)
        {
            final AbstractSubscriberInvoker<?> invoker = invokerByName.get(subscriberName);
            if (invoker != null)
            {
                invokersBySessionId.put(session.id(), invoker);
            }

            if (subscriberName.endsWith("__EgressSubscriber"))
            {
                clientSessionPublicationByName.get(subscriberName.split("__")[0] + "__EgressPublisher").bindClientSession(session);
            }
        }
    }

    public void onSessionMessage(final ClientSession session, final DirectBuffer buffer, final int offset)
    {
        invokersBySessionId.getOrDefault(session.id(), NullSubscriberInvokerImpl.INSTANCE).handle(buffer, offset);
    }
}
