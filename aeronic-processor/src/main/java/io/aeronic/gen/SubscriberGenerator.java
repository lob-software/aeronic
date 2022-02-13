package io.aeronic.gen;

import java.util.List;

public class SubscriberGenerator
{
    public String generate(final String packageName, final String interfaceName, final List<MethodInfo> methods)
    {
        return new StringBuilder()
            .append(generatePackageAndImports(packageName, interfaceName))
            .append(generateClassDeclaration(interfaceName))
            .append("\n").append("{").append("\n")
            .append(generateConstructor(interfaceName))
            .append(generateHandleMethod(methods))
            .append(generateRoleNameMethod(interfaceName))
            .append("}").append("\n")
            .toString();
    }

    private String generateHandleMethod(final List<MethodInfo> methods)
    {
        final StringBuilder handleMethodBuilder = new StringBuilder("""
                public void handle(final BufferDecoder bufferDecoder, final int offset)
                {
                    final int msgType = bufferDecoder.decodeInt();
                    switch (msgType)
                    {
            """);

        for (final MethodInfo interfaceMethod : methods)
        {
            final String methodName = interfaceMethod.getName();
            final List<ParameterInfo> parameters = interfaceMethod.getParameters();
            for (final ParameterInfo parameter : parameters)
            {
                final String parameterName = parameter.getName();
                if (parameter.getType().equals(long.class.getName()))
                {
                    handleMethodBuilder.append("""
                                    case %s -> {
                                        final long %s = bufferDecoder.decodeLong();
                                        subscriber.%s(%s);
                                    }
                        """.formatted(interfaceMethod.getIndex(), parameterName, methodName, parameterName));
                }
            }
        }

        handleMethodBuilder.append("""
                        default -> throw new RuntimeException("Unexpected message type: " + msgType);
                    }
                }
            """);

        return handleMethodBuilder.toString();
    }

    private String generateConstructor(final String interfaceName)
    {
        return """
                
                public %sSubscriber(final Subscription subscription, final %s subscriber)
                {
                    super(subscription, subscriber);
                }
                        
            """.formatted(interfaceName, interfaceName);
    }

    private String generateClassDeclaration(final String interfaceName)
    {
        return "public class %sSubscriber extends AbstractSubscriber<%s>".formatted(interfaceName, interfaceName);
    }

    private String generateRoleNameMethod(final String interfaceName)
    {
        return """
                            
                @Override
                public String roleName()
                {
                    return "%s";
                }
            """.formatted(interfaceName);
    }

    private String generatePackageAndImports(final String packageName, final String interfaceName)
    {
        return """
            package %s;
                    
            import %s.%s;
            import io.aeron.Subscription;
            import io.aeronic.net.AbstractSubscriber;
            import io.aeronic.net.BufferDecoder;
            import org.agrona.BitUtil;
            import org.agrona.DirectBuffer;
                    
            """.formatted(packageName, packageName, interfaceName);
    }
}
