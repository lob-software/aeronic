[![build](https://github.com/eliquinox/aeronic/actions/workflows/gradle.yml/badge.svg)](https://github.com/eliquinox/aeronic/actions/workflows/gradle.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
# aeronic

Usage:

```kotlin
repositories {
    mavenCentral()
    maven {
        setUrl("https://dl.cloudsmith.io/public/lob-software/aeronic/maven/")
    }
}

dependencies {
    annotationProcessor("io.aeronic:aeronic:0.0.8")
    implementation("io.aeronic:aeronic:0.0.8")
}
```

## Quickstart

Aeronic allows for flexible usage of [Aeron](https://github.com/real-logic/simple-binary-encoding) by way of proxy generation for 
subscriptions and publications. Use `@Aeronic` to make the compiler generate subscriber and publisher proxies:

```java
@Aeronic
public interface TradeEvents
{
    void onTrade(long price);
}
```

A subscriber, defining business logic can then be defined by implementing the interface above:

```java
public class TradeEventsImpl implements TradeEvents
{

    private long lastPrice;

    @Override
    public void onTrade(final long price)
    {
        this.lastPrice = price;
    }
    
    public long getLastPrice()
    {
        return lastPrice;
    }
}
```

`AeronicWizard` can then be used to create a publisher of type `TradeEvents` and bind a subscriber implemented above. 
The two will communicate via a given Aeron channel / stream ID:

```java
final Aeron aeron = Aeron.connect(aeronCtx);
final AeronicWizard aeronic = new AeronicWizard(aeron);

final TradeEvents eventsPublisher = aeronic.createPublisher(TradeEvents.class, "aeron:ipc", 10);
final TradeEventsImpl subscriberImpl = new TradeEventsImpl();
aeronic.registerSubscriber(TradeEvents.class, subscriberImpl, "aeron:ipc", 10);

publisher.onTrade(123L);
subscriberImpl.getLastPrice(); // 123L
```
