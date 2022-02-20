[![Build](https://github.com/eliquinox/aeronic/actions/workflows/gradle.yml/badge.svg)](https://github.com/eliquinox/aeronic/actions/workflows/gradle.yml)

# aeronic

Aeronic allows for flexible usage of [Aeron](https://github.com/real-logic/simple-binary-encoding):

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

Aeron aeron = Aeron.connect(aeronCtx);
AeronicWizard aeronic = new AeronicWizard(aeron);

final Events eventsPublisher = aeronic.createPublisher(Events.class);
final EventsImpl subscriberImpl = new EventsImpl();
aeronic.registerSubscriber(Events.class, subscriberImpl);

publisher.onEvent(123L);
subscriberImpl.getValue(); // 123L
```


🚧🚧🚧 **Work In Progress** 🚧🚧🚧

Features in the pipeline:
* multiple event method parameters
* all primitives
* custom type parameters
* aeron cluster abstractions