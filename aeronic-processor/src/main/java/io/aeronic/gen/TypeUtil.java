package io.aeronic.gen;

import java.util.List;

public final class TypeUtil
{
    private static List<String> PRIMITIVES = List.of(
        "char",
        "byte",
        "short",
        "int",
        "float",
        "double",
        "long"
    );

    private TypeUtil()
    {
    }

    public static boolean isPrimitive(final String type)
    {
        return PRIMITIVES.contains(type);
    }

    public static String extractClassName(final String fullyQualifiedType)
    {
        final String[] split = fullyQualifiedType.split("\\.");
        return split[split.length - 1];
    }
}
