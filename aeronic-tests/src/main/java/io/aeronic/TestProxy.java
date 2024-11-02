package io.aeronic;

import java.lang.reflect.Proxy;

public class TestProxy
{

    @SuppressWarnings("unchecked")
    public static <T> T forInterface(final T aeronicInterface)
    {
        return (T)Proxy.newProxyInstance(
            aeronicInterface.getClass().getClassLoader(),
            aeronicInterface.getClass().getInterfaces(),
            (proxy, method, args) -> method.invoke(aeronicInterface, args)
        );
    }
}
