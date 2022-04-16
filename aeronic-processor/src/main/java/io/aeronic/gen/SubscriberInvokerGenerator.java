package io.aeronic.gen;

import java.util.List;

import static io.aeronic.gen.StringUtil.capitalize;
import static io.aeronic.gen.TypeUtil.isPrimitive;

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
            new StringBuilder("                subscriber.%s(\n".formatted(methodName));

        handleMethodBuilder.append("""
                        case %s -> {
            """.formatted(interfaceMethod.getIndex()));

        for (int i = 0; i < parameters.size(); i++)
        {
            writeParameter(handleMethodBuilder, subscriberInvocation, parameters.get(i), classImports);

            if (i < parameters.size() - 1)
            {
                subscriberInvocation.append(",\n");
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
        final String parameterName = parameter.getName();
        final String parameterType = parameter.getType();
        if (parameter.isPrimitive())
        {
            handleMethodBuilder.append("""
                                final %s %s = bufferDecoder.decode%s();
                """.formatted(parameterType, parameterName, capitalize(parameterType)));
            subscriberInvocation.append("                    %s".formatted(parameterName));
            return;
        }

        if (parameter.isArray())
        {
            final String arrayType = parameterType.substring(0, parameterType.length() - 2);
            if (isPrimitive(arrayType))
            {
                handleMethodBuilder.append("""
                                final %s %s = bufferDecoder.decode%sArray();
                """.formatted(parameterType, parameterName, capitalize(arrayType)));
            }
            else
            {
                final String className = TypeUtil.extractClassName(arrayType);
                handleMethodBuilder.append("""
                                final %s[] %s = bufferDecoder.decode(%s::decode, %s[]::new);
                """.formatted(className, parameterName, className, className));
            }

            subscriberInvocation.append("                    %s".formatted(parameterName));
            return;
        }

        if (parameterType.equals(String.class.getName()))
        {
            handleMethodBuilder.append("""
                                final String %s = bufferDecoder.decodeString();
                """.formatted(parameterName));
            subscriberInvocation.append("                    %s".formatted(parameterName));
            return;
        }

        final String[] split = parameterType.split("\\.");
        final String className = split[split.length - 1];

        handleMethodBuilder.append("""
                            final %s %s = %s.decode(bufferDecoder);
            """.formatted(className, parameterName, className));
        subscriberInvocation.append("                    %s".formatted(parameterName));
        classImports.append("import %s;".formatted(parameterType));
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
