package io.aeronic.codec;

import org.agrona.ExpandableArrayBuffer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class CodecTest
{
    @Test
    public void shouldEncodeAndDecode()
    {
        final SimpleImpl expected = new SimpleImpl(123, (byte)13, Long.MAX_VALUE);
        assertCodec(expected);
    }

    private static <T> void assertReflectiveEquals(final Object expected, final T actual)
    {
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    private static <T> void assertCodec(final Object object)
    {
        try
        {
            final Method[] methods = object.getClass().getMethods();
            final Method encoderMethod = Arrays.stream(methods)
                .filter(method -> method.isAnnotationPresent(Encoder.class) && method.getName().equals("encode"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(object + " does not properly define @Encoder method"));

            final Method decoderMethod = Arrays.stream(methods)
                .filter(method -> method.isAnnotationPresent(Decoder.class) && method.getName().equals("decode"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(object + " does not properly define @Decoder method"));

            final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(128);
            encoderMethod.invoke(object, buffer);

            final Object decoded = decoderMethod.invoke(null, buffer);

            assertReflectiveEquals(object, decoded);
        }
        catch (final Exception e)
        {
            throw new AssertionError(e);
        }
    }
}
