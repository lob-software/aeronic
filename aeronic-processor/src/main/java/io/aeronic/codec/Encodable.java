package io.aeronic.codec;


@FunctionalInterface
public interface Encodable {
    void encode(BufferEncoder bufferEncoder);
}
