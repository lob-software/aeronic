package io.aeronic.codec;


import io.aeronic.net.BufferEncoder;

@FunctionalInterface
public interface Encoder
{
    void encode(BufferEncoder bufferEncoder);
}
