package io.aeronic.net;

import io.aeron.archive.client.ReplayMerge;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.Agent;

public class ReplayMergeAgent<T> implements Agent {
    private final ReplayMerge replayMerge;
    private final AbstractSubscriberInvoker<T> invoker;
    private final FragmentHandler fragmentHandler = new FragmentHandler() {
        @Override
        public void onFragment(final DirectBuffer buffer, final int offset, final int length, final Header header)
        {
            invoker.handle(buffer, offset);
        }
    };

    public ReplayMergeAgent(final ReplayMerge replayMerge, final AbstractSubscriberInvoker<T> invoker)
    {
        this.replayMerge = replayMerge;
        this.invoker = invoker;
    }

    @Override
    public int doWork()
    {
        return replayMerge.poll(fragmentHandler, 100);
    }

    @Override
    public String roleName()
    {
        return "replay-merge-agent";
    }
}
