package io.aeronic.codec;


@FunctionalInterface
public interface Encoder<T> {
    void encode(BufferEncoder bufferEncoder, T toEncode);
}
