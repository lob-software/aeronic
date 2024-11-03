package io.aeronic;

import io.aeronic.codec.BufferDecoder;
import io.aeronic.codec.BufferEncoder;
import io.aeronic.codec.Encodable;
import org.agrona.ExpandableDirectByteBuffer;
import org.awaitility.core.ThrowingRunnable;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class Assertions
{
    public static <T> void assertReflectiveEquals(final Object expected, final T actual)
    {
        assertThat(actual).usingRecursiveComparison()
            .isEqualTo(expected);
    }

    public static void assertCodec(final Object object)
    {
        try
        {
            final Class<?> clazz = object.getClass();

            Arrays.stream(clazz.getInterfaces())
                .filter(i -> i.equals(Encodable.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError(object + " does not implement Encodable interface"));

            final Method[] methods = clazz.getMethods();
            final Method encoderMethod = Arrays.stream(methods)
                .filter(method -> method.getName()
                    .equals("encode"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("This should never have happened..."));

            final Method decoderMethod = Arrays.stream(methods)
                .filter(method -> method.getName()
                    .equals("decode"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(object + " does not properly define @Decoder method"));

            final ExpandableDirectByteBuffer buffer = new ExpandableDirectByteBuffer();
            final BufferEncoder bufferEncoder = new BufferEncoder(buffer);
            final BufferDecoder bufferDecoder = new BufferDecoder();
            bufferDecoder.wrap(buffer, 0);

            encoderMethod.invoke(object, bufferEncoder);

            final Object decoded = decoderMethod.invoke(null, bufferDecoder);

            assertReflectiveEquals(object, decoded);
        }
        catch (final
        Exception e)
        {
            throw new AssertionError(e);
        }
    }

    public static void assertEventuallyTrue(final BooleanSupplier assertion, final long timeoutInMillis)
    {
        await()
            .pollInterval(Duration.ofMillis(50))
            .atMost(Duration.ofMillis(timeoutInMillis))
            .until(assertion::getAsBoolean);
    }

    public static void assertEventuallyTrue(final BooleanSupplier assertion)
    {
        assertEventuallyTrue(assertion, 2000L);
    }

    public static void assertEventually(final ThrowingRunnable runnable)
    {
        await()
            .pollInterval(Duration.ofMillis(50))
            .atMost(Duration.ofSeconds(1))
            .untilAsserted(runnable);
    }
}
