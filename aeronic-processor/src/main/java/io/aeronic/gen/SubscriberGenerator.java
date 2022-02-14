package io.aeronic.gen;

import java.util.List;

import static io.aeronic.gen.StringUtil.capitalize;

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
            writeMethodCase(handleMethodBuilder, interfaceMethod);
        }

        handleMethodBuilder.append("""
                        default -> throw new RuntimeException("Unexpected message type: " + msgType);
                    }
                }
            """);

        return handleMethodBuilder.toString();
    }

    private void writeMethodCase(final StringBuilder handleMethodBuilder, final MethodInfo interfaceMethod)
    {
        final String methodName = interfaceMethod.getName();
        final List<ParameterInfo> parameters = interfaceMethod.getParameters();
        final StringBuilder subscriberInvocation =
            new StringBuilder("                subscriber.%s(".formatted(methodName));

        handleMethodBuilder.append("""
                        case %s -> {
            """.formatted(interfaceMethod.getIndex()));

        for (int i = 0; i < parameters.size(); i++)
        {
            writeParameter(handleMethodBuilder, subscriberInvocation, parameters.get(i));

            if (i < parameters.size() - 1)
            {
                subscriberInvocation.append(", ");
            }
        }

        subscriberInvocation.append("""
            );
            """);

        handleMethodBuilder.append(subscriberInvocation);
        handleMethodBuilder.append("""
                        }
            """);
    }

    private void writeParameter(final StringBuilder handleMethodBuilder, final StringBuilder subscriberInvocation, final ParameterInfo parameter)
    {
        handleMethodBuilder.append("""
                            final %s %s = bufferDecoder.decode%s();
            """.formatted(parameter.getType(), parameter.getName(), capitalize(parameter.getType())));
        subscriberInvocation.append(parameter.getName());
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
            import io.aeronic.codec.BufferDecoder;
            import org.agrona.BitUtil;
            import org.agrona.DirectBuffer;
                    
            """.formatted(packageName, packageName, interfaceName);
    }
}
