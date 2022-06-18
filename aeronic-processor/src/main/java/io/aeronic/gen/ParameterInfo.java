package io.aeronic.gen;

import java.util.List;

public class ParameterInfo
{
    private final String name;
    private final String type;
    private final boolean isPrimitive;
    private final boolean isArray;
    private final List<String> genericParameters;

    public ParameterInfo(final String name, final String type, final boolean isPrimitive, final boolean isArray, final List<String> genericParameters)
    {
        this.name = name;
        this.type = type;
        this.isPrimitive = isPrimitive;
        this.isArray = isArray;
        this.genericParameters = genericParameters;
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

    public List<String> getGenericParameters()
    {
        return genericParameters;
    }

    @Override
    public String toString()
    {
        return "ParameterInfo{" +
            "name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", isPrimitive=" + isPrimitive +
            ", isArray=" + isArray +
            ", genericParameters=" + genericParameters +
            '}';
    }
}
