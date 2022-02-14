package io.aeronic;

import io.aeronic.codec.Encoder;
import io.aeronic.net.BufferDecoder;
import io.aeronic.net.BufferEncoder;
import org.agrona.ExpandableDirectByteBuffer;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class Assertions
{
    public static <T> void assertReflectiveEquals(final Object expected, final T actual)
    {
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    public static void assertCodec(final Object object)
    {
        try
        {
            final Class<?> clazz = object.getClass();

            Arrays.stream(clazz.getInterfaces())
                .filter(i -> i.equals(Encoder.class))
                .findFirst()
                .orElseThrow(() -> new AssertionError(object + " does not implement Encoder interface"));

            final Method[] methods = clazz.getMethods();
            final Method encoderMethod = Arrays.stream(methods)
                .filter(method -> method.getName().equals("encode"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("This should never have happened..."));

            final Method decoderMethod = Arrays.stream(methods)
                .filter(method -> method.getName().equals("decode"))
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
        catch (final Exception e)
        {
            throw new AssertionError(e);
        }
    }
}
