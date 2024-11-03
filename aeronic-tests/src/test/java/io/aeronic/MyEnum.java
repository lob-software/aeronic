package io.aeronic;

import io.aeronic.codec.BufferDecoder;
import io.aeronic.codec.BufferEncoder;
import io.aeronic.codec.DecodedBy;
import io.aeronic.codec.Encodable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public enum MyEnum implements Encodable
{
    ONE('a'), TWO('b'), THREE('c');

    private static final Map<Character, MyEnum> VALUES = Arrays.stream(values())
        .collect(toMap(e -> e.value, Function.identity()));
    private final char value;

    MyEnum(final char value)
    {
        this.value = value;
    }

    @Override
    public void encode(final BufferEncoder bufferEncoder)
    {
        bufferEncoder.encode(value);
    }

    @DecodedBy
    public static MyEnum decode(final BufferDecoder bufferDecoder)
    {
        final char value = bufferDecoder.decodeChar();
        return VALUES.get(value);
    }
}
