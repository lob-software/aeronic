package io.aeronic.gen;

import java.util.ArrayList;
import java.util.List;

import static io.aeronic.gen.StringUtil.capitalize;
import static io.aeronic.gen.TypeUtil.isPrimitive;

public class SubscriberInvokerGenerator
{
    private final List<String> imports = new ArrayList<>();

    private void addImport(final String importStatement)
    {
        if (!imports.contains(importStatement))
        {
            imports.add(importStatement);
        }
    }

    public String generate(final String packageName, final String interfaceName, final List<MethodInfo> methods)
    {
        final String handleMethod = generateHandleMethod(methods);

        return new StringBuilder()
            .append(generatePackageAndImports(packageName, interfaceName))
            .append(generateClassDeclaration(interfaceName))
            .append("\n").append("{").append("\n")
            .append(generateConstructor(interfaceName))
            .append(handleMethod)
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
            new StringBuilder("                subscriber.%s(\n".formatted(methodName));

        handleMethodBuilder.append("""
                        case %s -> {
            """.formatted(interfaceMethod.getIndex()));

        for (int i = 0; i < parameters.size(); i++)
        {
            writeParameter(handleMethodBuilder, subscriberInvocation, parameters.get(i));

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
        final ParameterInfo parameter
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
                                final %s[] %s = bufferDecoder.decodeArray(%s::decode, %s[]::new);
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

        final List<String> genericParameters = parameter.getGenericParameters();

        if (!genericParameters.isEmpty())
        {
            final String genericParameter = genericParameters.get(0);
            final String genericParameterClassName = TypeUtil.extractClassName(genericParameter);
            final String fullyQualifiedType = parameterType.split("<")[0];
            final String className = TypeUtil.extractClassName(fullyQualifiedType);

            handleMethodBuilder.append("""
                                final %s<%s> %s = bufferDecoder.decodeList(%s::decode, ArrayList::new);
                """.formatted(className, genericParameterClassName, parameterName, genericParameterClassName));
            subscriberInvocation.append("                    %s".formatted(parameterName));

            addImport("import %s;".formatted(genericParameter));
            addImport("import %s;".formatted(fullyQualifiedType));
            addImport("import java.util.ArrayList;");
            return;
        }

        final String className = TypeUtil.extractClassName(parameterType);
        handleMethodBuilder.append("""
                            final %s %s = %s.decode(bufferDecoder);
            """.formatted(className, parameterName, className));
        subscriberInvocation.append("                    %s".formatted(parameterName));
        addImport("import %s;".formatted(parameterType));
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

    private String generatePackageAndImports(final String packageName, final String interfaceName)
    {
        final String importsString = imports.stream().reduce("", (e, n) -> e + "\n" + n);
        return """
            package %s;
                    
            import %s.%s;
            import io.aeron.Subscription;
            import io.aeronic.net.AbstractSubscriberInvoker;
            import io.aeronic.codec.BufferDecoder;
            import org.agrona.BitUtil;
            import org.agrona.DirectBuffer;%s
            
                    
            """.formatted(packageName, packageName, interfaceName, importsString);
    }
}
