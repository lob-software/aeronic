package io.aeronic.gen;

import java.util.List;

public class MethodInfo
{
    private final int index;
    private final String name;
    private final List<ParameterInfo> parameters;

    public MethodInfo(final int index, final String name, final List<ParameterInfo> parameters)
    {
        this.index = index;
        this.name = name;
        this.parameters = parameters;
    }

    public int getIndex()
    {
        return index;
    }

    public String getName()
    {
        return name;
    }

    public List<ParameterInfo> getParameters()
    {
        return parameters;
    }
}
