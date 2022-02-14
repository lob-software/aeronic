package io.aeronic.codec;


@FunctionalInterface
public interface Encoder
{
    void encode(BufferEncoder bufferEncoder);
}
