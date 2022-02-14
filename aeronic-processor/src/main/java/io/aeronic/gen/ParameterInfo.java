package io.aeronic.gen;

public class ParameterInfo
{
    private final String name;
    private final String type;
    private final boolean isPrimitive;

    public ParameterInfo(final String name, final String type, final boolean isPrimitive)
    {
        this.name = name;
        this.type = type;
        this.isPrimitive = isPrimitive;
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        return type;
    }

    public boolean isPrimitive()
    {
        return isPrimitive;
    }
}
