package io.aeronic.gen;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor14;
import java.util.*;

public class AeronicInterfaceHelper
{
    private final Map<String, List<MethodInfo>> methodInfoByElementName = new HashMap<>();
    private final GenericParametersExtractor genericParametersExtractor = new GenericParametersExtractor();

    public void processEnvironment(final RoundEnvironment roundEnvironment)
    {
        final Set<? extends Element> aeronicElements = roundEnvironment.getElementsAnnotatedWith(Aeronic.class);

        for (final Element aeronicElement : aeronicElements)
        {
            final String elementName = aeronicElement.getSimpleName().toString();
            final List<? extends Element> enclosedElements = aeronicElement.getEnclosedElements();

            int methodIndex = 0;
            final List<MethodInfo> methods = new ArrayList<>();
            for (final Element enclosedElement : enclosedElements)
            {
                final ExecutableElement methodElement = (ExecutableElement) enclosedElement;
                final List<? extends VariableElement> params = methodElement.getParameters();

                final List<ParameterInfo> parameters = new ArrayList<>();
                for (final VariableElement param : params)
                {
                    final List<String> genericParameters = param.asType().accept(genericParametersExtractor, null);

                    final TypeKind typeKind = param.asType().getKind();
                    final boolean isPrimitive = typeKind.isPrimitive();
                    final boolean isArray = typeKind == TypeKind.ARRAY;
                    final ParameterInfo parameter = new ParameterInfo(
                            param.getSimpleName().toString(),
                            param.asType().toString(),
                            isPrimitive,
                            isArray,
                            genericParameters
                    );

                    parameters.add(parameter);
                }

                final MethodInfo method = new MethodInfo(methodIndex++, methodElement.getSimpleName().toString(), parameters);
                methods.add(method);
            }

            methodInfoByElementName.put(elementName, methods);
        }
    }

    public List<MethodInfo> getMethodInfoFor(final String elementName)
    {
        return methodInfoByElementName.get(elementName);
    }

    private static final class GenericParametersExtractor extends SimpleTypeVisitor14<List<String>, Void>
    {

        GenericParametersExtractor()
        {
        }

        @Override
        public List<String> visitDeclared(final DeclaredType type, final Void unused)
        {
            return type.getTypeArguments().stream().map(TypeMirror::toString).toList();
        }
    }
}
