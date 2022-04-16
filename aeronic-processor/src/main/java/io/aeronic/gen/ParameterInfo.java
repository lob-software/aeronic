package io.aeronic.gen;

public class ParameterInfo
{
    private final String name;
    private final String type;
    private final boolean isPrimitive;
    private final boolean isArray;

    public ParameterInfo(final String name, final String type, final boolean isPrimitive, final boolean isArray)
    {
        this.name = name;
        this.type = type;
        this.isPrimitive = isPrimitive;
        this.isArray = isArray;
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

    public boolean isArray()
    {
        return isArray;
    }
}
