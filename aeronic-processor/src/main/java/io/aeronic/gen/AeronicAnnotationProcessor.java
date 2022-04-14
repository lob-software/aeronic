package io.aeronic.gen;



import com.google.auto.service.AutoService;
import io.aeronic.Aeronic;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("io.aeronic.Aeronic")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class AeronicAnnotationProcessor extends AbstractProcessor
{
    private final SubscriberInvokerGenerator subscriberInvokerGenerator = new SubscriberInvokerGenerator();
    private final PublisherGenerator publisherGenerator = new PublisherGenerator();

    @Override
    public boolean process(final Set<? extends TypeElement> set, final RoundEnvironment roundEnvironment)
    {
        final Set<? extends Element> aeronicElements = roundEnvironment.getElementsAnnotatedWith(Aeronic.class);

        for (final Element aeronicElement : aeronicElements)
        {
            final String packageName = processingEnv.getElementUtils().getPackageOf(aeronicElement).getQualifiedName().toString();
            final String elementName = aeronicElement.getSimpleName().toString();
            final List<? extends Element> enclosedElements = aeronicElement.getEnclosedElements();

            int methodIndex = 0;
            final List<MethodInfo> methods = new ArrayList<>();
            for (final Element enclosedElement : enclosedElements)
            {
                final ExecutableElement methodElement = (ExecutableElement)enclosedElement;
                final List<? extends VariableElement> params = methodElement.getParameters();

                final List<ParameterInfo> parameters = new ArrayList<>();
                for (final VariableElement param : params)
                {
                    final boolean isPrimitive = param.asType().getKind().isPrimitive();
                    final ParameterInfo parameter = new ParameterInfo(param.getSimpleName().toString(), param.asType().toString(), isPrimitive);
                    parameters.add(parameter);
                }

                final MethodInfo method = new MethodInfo(methodIndex++, methodElement.getSimpleName().toString(), parameters);
                methods.add(method);
            }

            final String invokerSource = subscriberInvokerGenerator.generate(packageName, elementName, methods);
            try
            {
                final String invokerPath = "%s.%sInvoker".formatted(packageName, elementName);
                final JavaFileObject invokerSourceFile = processingEnv.getFiler().createSourceFile(invokerPath, aeronicElement);
                final Writer writer = invokerSourceFile.openWriter();
                writer.append(invokerSource);
                writer.close();
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Written file for %s".formatted(invokerPath));
            }
            catch (final IOException e)
            {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "Could not create source file: " + e.getMessage(), aeronicElement
                );
            }

            final String publisherSource = publisherGenerator.generate(packageName, elementName, methods);
            try
            {
                final String publisherPath = "%s.%sPublisher".formatted(packageName, elementName);
                final JavaFileObject publisherSourceFile = processingEnv.getFiler().createSourceFile(publisherPath, aeronicElement);
                final Writer writer = publisherSourceFile.openWriter();
                writer.append(publisherSource);
                writer.close();
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Written file for %s".formatted(publisherPath));
            }
            catch (final IOException e)
            {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "Could not create source file: " + e.getMessage(), aeronicElement
                );
            }

        }

        return true;
    }
}

