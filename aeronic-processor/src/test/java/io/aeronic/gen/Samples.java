package io.aeronic.gen;

public class Samples {
    public static final String SAMPLE_SUBSCRIBER =
            """
                    package io.aeronic;
                    
                    import io.aeronic.TestEvents;
                    import io.aeron.Subscription;
                    import io.aeronic.net.AbstractSubscriberInvoker;
                    import io.aeronic.codec.BufferDecoder;
                    import org.agrona.BitUtil;
                    import org.agrona.DirectBuffer;
                    import io.aeronic.codec.SimpleImpl;
                    import io.aeronic.MyEnum;
                    import java.util.List;
                    import java.util.ArrayList;
                    
                    
                    public class TestEventsInvoker extends AbstractSubscriberInvoker<TestEvents>
                    {
                    
                        public TestEventsInvoker(final TestEvents subscriber)
                        {
                            super(subscriber);
                        }
                    
                        public void handle(final BufferDecoder bufferDecoder, final int offset)
                        {
                            final int msgType = bufferDecoder.decodeInt();
                            switch (msgType)
                            {
                                case 0 -> {
                                    final long aLong = bufferDecoder.decodeLong();
                                    final int intValue = bufferDecoder.decodeInt();
                                    final float floatValue = bufferDecoder.decodeFloat();
                                    final double doubleValue = bufferDecoder.decodeDouble();
                                    final byte byteValue = bufferDecoder.decodeByte();
                                    final char charValue = bufferDecoder.decodeChar();
                                    final SimpleImpl simpleImpl = SimpleImpl.decode(bufferDecoder);
                                    final String stringValue = bufferDecoder.decodeString();
                                    final long[] longs = bufferDecoder.decodeLongArray();
                                    final int[] ints = bufferDecoder.decodeIntArray();
                                    final float[] floats = bufferDecoder.decodeFloatArray();
                                    final double[] doubles = bufferDecoder.decodeDoubleArray();
                                    final byte[] bytes = bufferDecoder.decodeByteArray();
                                    final char[] chars = bufferDecoder.decodeCharArray();
                                    final SimpleImpl[] simples = bufferDecoder.decodeArray(SimpleImpl::decode, SimpleImpl[]::new);
                                    final MyEnum myEnum = MyEnum.decode(bufferDecoder);
                                    final List<SimpleImpl> simpleList = bufferDecoder.decodeList(SimpleImpl::decode, ArrayList::new);
                                    subscriber.onEvent(
                                        aLong,
                                        intValue,
                                        floatValue,
                                        doubleValue,
                                        byteValue,
                                        charValue,
                                        simpleImpl,
                                        stringValue,
                                        longs,
                                        ints,
                                        floats,
                                        doubles,
                                        bytes,
                                        chars,
                                        simples,
                                        myEnum,
                                        simpleList
                                    );
                                }
                                case 1 -> {
                                    final long timestamp = bufferDecoder.decodeLong();
                                    subscriber.onTimer(
                                        timestamp
                                    );
                                }
                            }
                        }
                    }     
                    """;


    public static final String SAMPLE_PUBLISHER =
            """
                    package io.aeronic;
                    
                    import io.aeron.Publication;
                    import io.aeronic.net.AbstractPublisher;
                    import io.aeronic.net.AeronicPublication;
                    import org.agrona.BitUtil;
                    import io.aeronic.codec.SimpleImpl;
                    import io.aeronic.MyEnum;
                    import java.util.List;
                    
                    
                    public class TestEventsPublisher extends AbstractPublisher implements TestEvents
                    {
                    
                        public TestEventsPublisher(final AeronicPublication publication)
                        {
                            super(publication);
                        }
                    
                        @Override
                        public void onEvent(
                            final long aLong,
                            final int intValue,
                            final float floatValue,
                            final double doubleValue,
                            final byte byteValue,
                            final char charValue,
                            final SimpleImpl simpleImpl,
                            final String stringValue,
                            final long[] longs,
                            final int[] ints,
                            final float[] floats,
                            final double[] doubles,
                            final byte[] bytes,
                            final char[] chars,
                            final SimpleImpl[] simples,
                            final MyEnum myEnum,
                            final List<SimpleImpl> simpleList
                        )
                        {
                            bufferEncoder.encode(0);
                            bufferEncoder.encode(aLong);
                            bufferEncoder.encode(intValue);
                            bufferEncoder.encode(floatValue);
                            bufferEncoder.encode(doubleValue);
                            bufferEncoder.encode(byteValue);
                            bufferEncoder.encode(charValue);
                            simpleImpl.encode(bufferEncoder);
                            bufferEncoder.encode(stringValue);
                            bufferEncoder.encode(longs);
                            bufferEncoder.encode(ints);
                            bufferEncoder.encode(floats);
                            bufferEncoder.encode(doubles);
                            bufferEncoder.encode(bytes);
                            bufferEncoder.encode(chars);
                            bufferEncoder.encode(simples);
                            myEnum.encode(bufferEncoder);
                            bufferEncoder.encode(simpleList);
                            offer();
                        }
                    
                        @Override
                        public void onTimer(
                            final long timestamp
                        )
                        {
                            bufferEncoder.encode(1);
                            bufferEncoder.encode(timestamp);
                            offer();
                        }
                    }
                    """;
}
