package io.aeronic.gen;

public final class StringUtil {
    private StringUtil()
    {
    }

    public static String capitalize(final String str)
    {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
