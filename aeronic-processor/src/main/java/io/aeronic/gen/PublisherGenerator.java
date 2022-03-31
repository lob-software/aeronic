package io.aeronic.gen;

import java.util.List;

import static io.aeronic.gen.StringUtil.capitalize;

public class PublisherGenerator
{
    public String generate(final String packageName, final String interfaceName, final List<MethodInfo> methods)
    {
        final StringBuilder classImports = new StringBuilder();
        final String generatedMethods = generateMethods(methods, classImports);

        return new StringBuilder()
            .append(generatePackageAndImports(classImports, packageName))
            .append(generateClassDeclaration(interfaceName))
            .append("\n").append("{").append("\n")
            .append(generateConstructor(interfaceName))
            .append(generatedMethods)
            .append("}").append("\n")
            .toString();
    }

    private String generateMethods(final List<MethodInfo> methods, final StringBuilder packageAndImports)
    {
        final StringBuilder methodsBuilder = new StringBuilder();
        for (final MethodInfo interfaceMethod : methods)
        {
            final String methodName = interfaceMethod.getName();
            final List<ParameterInfo> parameters = interfaceMethod.getParameters();

            methodsBuilder.append("    @Override\n");
            methodsBuilder.append("    public void %s(\n".formatted(methodName));

            final StringBuilder methodBodyBuilder = new StringBuilder();
            methodBodyBuilder.append("""
                        bufferEncoder.encodeInt(0);
                """);

            for (int i = 0; i < parameters.size(); i++)
            {
                writeParameter(methodsBuilder, methodBodyBuilder, parameters.get(i), packageAndImports);

                if (i < parameters.size() - 1)
                {
                    methodsBuilder.append(",\n");
                }
            }

            methodBodyBuilder.append("""
                        offer();
                """);

            methodsBuilder.append("\n    )");
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

    private void writeParameter(final StringBuilder methodsBuilder, final StringBuilder methodBodyBuilder, final ParameterInfo parameter, final StringBuilder packageAndImports)
    {
        if (parameter.isPrimitive())
        {
            methodsBuilder.append("        final %s %s".formatted(parameter.getType(), parameter.getName()));
            methodBodyBuilder.append("""
                bufferEncoder.encode%s(%s);
        """.formatted(capitalize(parameter.getType()), parameter.getName()));
        }
        else
        {
            final String type = parameter.getType();
            if (type.equals(String.class.getName()))
            {
                methodsBuilder.append("        final String %s".formatted(parameter.getName()));
                methodBodyBuilder.append("""
                        bufferEncoder.encodeString(%s);
                """.formatted(parameter.getName()));
                return;
            }

            // TODO: is there a better way?
            final String[] split = type.split("\\.");
            final String className = split[split.length - 1];
            methodsBuilder.append("        final %s %s".formatted(className, parameter.getName()));
            methodBodyBuilder.append("""
                %s.encode(bufferEncoder);
        """.formatted(parameter.getName()));
            packageAndImports.append("import %s;".formatted(type));
        }
    }

    private String generateConstructor(final String interfaceName)
    {
        return """
                
                public %sPublisher(final AeronicPublication publication)
                {
                    super(publication);
                }
                
            """.formatted(interfaceName);
    }

    private String generateClassDeclaration(final String interfaceName)
    {
        return "public class %sPublisher extends AbstractPublisher implements %s".formatted(interfaceName, interfaceName);
    }

    private String generatePackageAndImports(final StringBuilder classImports, final String packageName)
    {
        return """
            package %s;
                    
            import io.aeron.Publication;
            import io.aeronic.net.AbstractPublisher;
            import io.aeronic.net.AeronicPublication;
            import org.agrona.BitUtil;
            %s
                        
            """.formatted(packageName, classImports);
    }
}
