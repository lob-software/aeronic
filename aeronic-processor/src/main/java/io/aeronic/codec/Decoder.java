package io.aeronic.codec;


@FunctionalInterface
public interface Decoder<T>
{
    T decode(BufferDecoder bufferDecoder);
}
