[![build](https://github.com/eliquinox/aeronic/actions/workflows/gradle.yml/badge.svg)](https://github.com/eliquinox/aeronic/actions/workflows/gradle.yml)

# aeronic

Aeronic allows for flexible usage of [Aeron](https://github.com/real-logic/simple-binary-encoding) by way of proxy generation for 
subscriptions and publications:

```java
@Aeronic
public interface Events
{
    void onEvent(long value);
}

public class EventsImpl implements Events
{

    private long value;

    @Override
    public void onEvent(final long value)
    {
        this.value = value;
    }
    
    public long getValue()
    {
        return value;
    }
}

final Aeron aeron = Aeron.connect(aeronCtx);
final AeronicWizard aeronic = new AeronicWizard(aeron);

final Events eventsPublisher = aeronic.createPublisher(Events.class, "aeron:ipc", 10);
final EventsImpl subscriberImpl = new EventsImpl();
aeronic.registerSubscriber(Events.class, subscriberImpl, "aeron:ipc", 10);

publisher.onEvent(123L);
subscriberImpl.getValue(); // 123L
```
