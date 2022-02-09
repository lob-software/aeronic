package io.aeronic;

import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeronic.gen.Aeronic;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.BusySpinIdleStrategy;

import java.util.ArrayList;
import java.util.List;

public class AeronicWizard
{
    private final Aeron aeron;

    private final List<AgentRunner> runners = new ArrayList<>();

    public AeronicWizard(final Aeron aeron)
    {
        this.aeron = aeron;
    }

    @SuppressWarnings("unchecked")
    public <T> T createPublisher(final Class<T> clazz, final String channel, final int streamId)
    {
        try
        {
            final Publication publication = aeron.addPublication(channel, streamId);
            return (T) Class.forName(clazz.getName() + "Publisher").getConstructor(Publication.class).newInstance(publication);
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public <T> void registerSubscriber(final Class<T> aeronicInterfaceClass, final T subscriberImplementation, final String channel, final int streamId)
    {
        try
        {
            final Subscription subscription = aeron.addSubscription(channel, streamId);
            final Agent subscriberAgent = (Agent) Class.forName(aeronicInterfaceClass.getName() + "Subscriber")
                .getConstructor(Subscription.class, aeronicInterfaceClass)
                .newInstance(subscription, subscriberImplementation);

            final AgentRunner receiveAgentRunner = new AgentRunner(new BusySpinIdleStrategy(), Throwable::printStackTrace, null, subscriberAgent);
            AgentRunner.startOnThread(receiveAgentRunner);
            runners.add(receiveAgentRunner);
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void close()
    {
        runners.forEach(AgentRunner::close);
    }
}
