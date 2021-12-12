package io.aeronic;

/**
 * Implementation to be invoked by receiver thread. User-provided.
 *
 */
public class ValueEventsListenerImpl implements ValueEvents
{
    private long value;
    private long valueOne;
    private long valueTwo;

    public ValueEventsListenerImpl(final long value, final long valueOne, final long valueTwo)
    {
        this.value = value;
        this.valueOne = valueOne;
        this.valueTwo = valueTwo;
    }

    @Override
    public void setValue(final long value)
    {
        this.value = value;
    }

    @Override
    public void setValues(final long valueOne, final long valueTwo)
    {
        this.valueOne = valueOne;
        this.valueTwo = valueTwo;
    }

    public long getValue()
    {
        return value;
    }

    public long getValueOne()
    {
        return valueOne;
    }

    public long getValueTwo()
    {
        return valueTwo;
    }

    @Override
    public String toString()
    {
        return "ValueEventsListenerImpl{" +
            "value=" + value +
            ", valueOne=" + valueOne +
            ", valueTwo=" + valueTwo +
            '}';
    }
}
