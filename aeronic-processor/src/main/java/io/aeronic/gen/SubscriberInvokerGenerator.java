package io.aeronic.gen;

import java.util.List;

import static io.aeronic.gen.StringUtil.capitalize;

public class SubscriberInvokerGenerator
{
    public String generate(final String packageName, final String interfaceName, final List<MethodInfo> methods)
    {
        final StringBuilder classImports = new StringBuilder();
        final String handleMethod = generateHandleMethod(methods, classImports);

        return new StringBuilder()
            .append(generatePackageAndImports(packageName, interfaceName, classImports))
            .append(generateClassDeclaration(interfaceName))
            .append("\n").append("{").append("\n")
            .append(generateConstructor(interfaceName))
            .append(handleMethod)
            .append("}").append("\n")
            .toString();
    }

    private String generateHandleMethod(final List<MethodInfo> methods, final StringBuilder classImports)
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
            writeMethodCase(handleMethodBuilder, interfaceMethod, classImports);
        }

        handleMethodBuilder.append("""
                        default -> throw new RuntimeException("Unexpected message type: " + msgType);
                    }
                }
            """);

        return handleMethodBuilder.toString();
    }

    private void writeMethodCase(final StringBuilder handleMethodBuilder, final MethodInfo interfaceMethod, final StringBuilder classImports)
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
            writeParameter(handleMethodBuilder, subscriberInvocation, parameters.get(i), classImports);

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

    private void writeParameter(
        final StringBuilder handleMethodBuilder,
        final StringBuilder subscriberInvocation,
        final ParameterInfo parameter,
        final StringBuilder classImports
    )
    {
        if (parameter.isPrimitive())
        {
            handleMethodBuilder.append("""
                            final %s %s = bufferDecoder.decode%s();
            """.formatted(parameter.getType(), parameter.getName(), capitalize(parameter.getType())));
            subscriberInvocation.append(parameter.getName());
        }
        else
        {
            if (parameter.getType().equals(String.class.getName()))
            {
                handleMethodBuilder.append("""
                                final String %s = bufferDecoder.decodeString();
                """.formatted(parameter.getName()));
                subscriberInvocation.append(parameter.getName());
                return;
            }

            final String[] split = parameter.getType().split("\\.");
            final String className = split[split.length - 1];

            handleMethodBuilder.append("""
                            final %s %s = %s.decode(bufferDecoder);
            """.formatted(className, parameter.getName(), className));
            subscriberInvocation.append(parameter.getName());
            classImports.append("import %s;".formatted(parameter.getType()));
        }
    }

    private String generateConstructor(final String interfaceName)
    {
        return """
                
                public %sInvoker(final %s subscriber)
                {
                    super(subscriber);
                }
                        
            """.formatted(interfaceName, interfaceName);
    }

    private String generateClassDeclaration(final String interfaceName)
    {
        return "public class %sInvoker extends AbstractSubscriberInvoker<%s>".formatted(interfaceName, interfaceName);
    }

    private String generatePackageAndImports(final String packageName, final String interfaceName, final StringBuilder classImports)
    {
        return """
            package %s;
                    
            import %s.%s;
            import io.aeron.Subscription;
            import io.aeronic.net.AbstractSubscriberInvoker;
            import io.aeronic.codec.BufferDecoder;
            import org.agrona.BitUtil;
            import org.agrona.DirectBuffer;
            %s
                    
            """.formatted(packageName, packageName, interfaceName, classImports);
    }
}
