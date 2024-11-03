package io.aeronic.gen;

import java.util.ArrayList;
import java.util.List;

import static io.aeronic.gen.TypeUtil.isPrimitive;

public class PublisherGenerator {
    private final List<String> imports = new ArrayList<>();

    private void addImport(final String importStatement)
    {
        if (!imports.contains(importStatement)) {
            imports.add(importStatement);
        }
    }

    public String generate(final String packageName, final String interfaceName, final List<MethodInfo> methods)
    {
        final String generatedMethods = generateMethods(methods);

        return new StringBuilder()
                .append(generatePackageAndImports(packageName))
                .append(generateClassDeclaration(interfaceName))
                .append("\n").append("{").append("\n")
                .append(generateConstructor(interfaceName))
                .append(generatedMethods)
                .append("}").append("\n")
                .toString();
    }

    private String generateMethods(final List<MethodInfo> methods)
    {
        final StringBuilder methodsBuilder = new StringBuilder();
        for (int i = 0; i < methods.size(); i++) {
            final MethodInfo interfaceMethod = methods.get(i);
            final String methodName = interfaceMethod.getName();
            final List<ParameterInfo> parameters = interfaceMethod.getParameters();

            methodsBuilder.append("    @Override\n");
            methodsBuilder.append("    public void %s(\n".formatted(methodName));

            final StringBuilder methodBodyBuilder = new StringBuilder();
            methodBodyBuilder.append("""
                                                     bufferEncoder.encode(%s);
                                             """.formatted(interfaceMethod.getIndex()));

            for (int j = 0; j < parameters.size(); j++) {
                writeParameter(methodsBuilder, methodBodyBuilder, parameters.get(j));

                if (j < parameters.size() - 1) {
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

            if (i < methods.size() - 1) {
                methodBodyBuilder.append("""
                                                     }
                                                 
                                                 """);
            }

            methodsBuilder.append(methodBodyBuilder);
        }

        methodsBuilder.append("""
                                          }
                                      """);

        return methodsBuilder.toString();
    }

    private void writeParameter(
            final StringBuilder methodsBuilder,
            final StringBuilder methodBodyBuilder,
            final ParameterInfo parameter
                               )
    {
        final String parameterType = parameter.getType();
        final String parameterName = parameter.getName();
        if (parameter.isPrimitive()) {
            methodsBuilder.append("        final %s %s".formatted(parameterType, parameterName));
            methodBodyBuilder.append("""
                                                     bufferEncoder.encode(%s);
                                             """.formatted(parameterName));
            return;
        }

        if (parameter.isArray()) {
            final String arrayType = parameterType.substring(0, parameterType.length() - 2);
            if (isPrimitive(arrayType)) {
                methodsBuilder.append("        final %s %s".formatted(parameterType, parameterName));
            } else {
                final String className = TypeUtil.extractClassName(arrayType);
                methodsBuilder.append("        final %s[] %s".formatted(className, parameterName));
            }
            methodBodyBuilder.append("""
                                                     bufferEncoder.encode(%s);
                                             """.formatted(parameterName));
            return;
        }

        if (parameterType.equals(String.class.getName())) {
            methodsBuilder.append("        final String %s".formatted(parameterName));
            methodBodyBuilder.append("""
                                                     bufferEncoder.encode(%s);
                                             """.formatted(parameterName));
            return;
        }

        final List<String> genericParameters = parameter.getGenericParameters();
        if (!genericParameters.isEmpty()) {
            final String genericParameter = genericParameters.get(0);
            final String genericParameterClassName = TypeUtil.extractClassName(genericParameter);
            final String fullyQualifiedType = parameterType.split("<")[0];
            final String className = TypeUtil.extractClassName(fullyQualifiedType);

            methodsBuilder.append("        final %s<%s> %s".formatted(className, genericParameterClassName, parameterName));
            methodBodyBuilder.append("""
                                                     bufferEncoder.encode(%s);
                                             """.formatted(parameterName));

            addImport("import %s;".formatted(genericParameter));
            addImport("import %s;".formatted(fullyQualifiedType));

            return;
        }

        final String className = TypeUtil.extractClassName(parameterType);
        methodsBuilder.append("        final %s %s".formatted(className, parameterName));
        methodBodyBuilder.append("""
                                                 %s.encode(bufferEncoder);
                                         """.formatted(parameterName));
        addImport("import %s;".formatted(parameterType));
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

    private String generatePackageAndImports(final String packageName)
    {
        final String importsString = imports.stream().reduce("", (e, n) -> e + "\n" + n);
        return """
                package %s;
                
                import io.aeron.Publication;
                import io.aeronic.net.AbstractPublisher;
                import io.aeronic.net.AeronicPublication;
                import org.agrona.BitUtil;%s
                
                
                """.formatted(packageName, importsString);
    }
}
