package io.aeronic.gen;

import java.util.List;

public class PublisherGenerator
{
    public String generate(final String packageName, final String interfaceName, final List<MethodInfo> methods)
    {
        return new StringBuilder()
            .append(generatePackageAndImports(packageName))
            .append(generateClassDeclaration(interfaceName))
            .append("\n").append("{").append("\n")
            .append(generateConstructor(interfaceName))
            .append(generateMethods(methods))
            .append("}").append("\n")
            .toString();
    }

    private String generateMethods(final List<MethodInfo> methods)
    {
        final StringBuilder methodsBuilder = new StringBuilder();
        for (final MethodInfo interfaceMethod : methods)
        {
            final String methodName = interfaceMethod.getName();
            final List<ParameterInfo> parameters = interfaceMethod.getParameters();

            methodsBuilder.append("    @Override\n");
            methodsBuilder.append("    public void %s(".formatted(methodName));

            final StringBuilder methodBodyBuilder = new StringBuilder();
            methodBodyBuilder.append("""
                        bufferEncoder.encodeInt(0);
                """);

            for (int i = 0; i < parameters.size(); i++)
            {
                final ParameterInfo parameter = parameters.get(i);
                writeParameter(methodsBuilder, methodBodyBuilder, parameter);

                if (i < parameters.size() - 1)
                {
                    methodsBuilder.append(", ");
                }
            }

            methodBodyBuilder.append("""
                        offer();
                """);

            methodsBuilder.append(")");
            methodsBuilder.append("""
                    
                    {
                """);

            methodsBuilder.append(methodBodyBuilder);
        }

        methodsBuilder.append("""
                }
            """);

        return methodsBuilder.toString();
    }

    private void writeParameter(final StringBuilder methodsBuilder, final StringBuilder methodBodyBuilder, final ParameterInfo parameter)
    {
        methodsBuilder.append("final %s %s".formatted(parameter.getType(), parameter.getName()));
        methodBodyBuilder.append("""
                bufferEncoder.encode%s(%s);
        """.formatted(capitalize(parameter.getType()), parameter.getName()));
    }

    private static String capitalize(final String str)
    {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String generateConstructor(final String interfaceName)
    {
        return """
                
                public %sPublisher(final Publication publication)
                {
                    super(publication);
                }
                
            """.formatted(interfaceName);
    }

    private String generateClassDeclaration(final String interfaceName)
    {
        return "public class %sPublisher extends AbstractPublisher implements %s".formatted(interfaceName, interfaceName);
    }

    private String generatePackageAndImports(final String packageName)
    {
        return """
            package %s;
                    
            import io.aeron.Publication;
            import io.aeronic.net.AbstractPublisher;
            import org.agrona.BitUtil;
                        
            import static io.aeronic.net.Constants.METHOD_IDX_OFFSET;
                        
            """.formatted(packageName);
    }
}
