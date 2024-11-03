package io.aeronic.gen;


import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("io.aeronic.gen.Aeronic")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class AeronicAnnotationProcessor extends AbstractProcessor
{
    private final SubscriberInvokerGenerator subscriberInvokerGenerator = new SubscriberInvokerGenerator();
    private final PublisherGenerator publisherGenerator = new PublisherGenerator();
    private final AeronicInterfaceHelper aeronicInterfaceHelper = new AeronicInterfaceHelper();

    @Override
    public boolean process(final Set<? extends TypeElement> set, final RoundEnvironment roundEnvironment)
    {
        final Set<? extends Element> aeronicElements = roundEnvironment.getElementsAnnotatedWith(Aeronic.class);

        aeronicInterfaceHelper.processEnvironment(roundEnvironment);

        for (final Element aeronicElement : aeronicElements)
        {
            final String packageName = processingEnv.getElementUtils().getPackageOf(aeronicElement).getQualifiedName().toString();
            final String elementName = aeronicElement.getSimpleName().toString();
            final List<MethodInfo> methods = aeronicInterfaceHelper.getMethodInfoFor(elementName);
            final String invokerSource = subscriberInvokerGenerator.generate(packageName, elementName, methods);
            try
            {
                final String invokerPath = "%s.%sInvoker".formatted(packageName, elementName);
                final JavaFileObject invokerSourceFile = processingEnv.getFiler().createSourceFile(invokerPath, aeronicElement);
                final Writer writer = invokerSourceFile.openWriter();
                writer.append(invokerSource);
                writer.close();
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Written file for %s".formatted(invokerPath));
            } catch (final IOException e)
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
            } catch (final IOException e)
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

