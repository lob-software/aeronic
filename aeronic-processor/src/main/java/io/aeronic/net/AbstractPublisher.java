package io.aeronic.net;

import io.aeronic.codec.BufferEncoder;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;

public abstract class AbstractPublisher {
    private final AeronicPublication publication;
    private final MutableDirectBuffer buffer;
    protected final BufferEncoder bufferEncoder;

    public AbstractPublisher(final AeronicPublication publication)
    {
        this.publication = publication;
        this.buffer = new ExpandableDirectByteBuffer(128);
        this.bufferEncoder = new BufferEncoder(buffer);
    }

    protected void offer()
    {
        if (publication.isConnected()) {
            long offerResult = publication.offer(buffer);
            if (offerResult < 0) {
                int remainingOfferAttempts = 10000;
                while (offerResult < 0) {
                    publication.onOfferFailure(offerResult);
                    offerResult = publication.offer(buffer);
                    remainingOfferAttempts--;
                    if (remainingOfferAttempts == 0) {
                        break;
                    }
                }
            }
        }

        bufferEncoder.reset();
    }
}
