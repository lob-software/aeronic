package io.aeronic.gen;

import com.google.auto.service.AutoService;
import io.aeronic.codec.BufferDecoder;
import io.aeronic.codec.BufferEncoder;
import io.aeronic.codec.DecodedBy;
import io.aeronic.codec.Encodable;
import org.joor.CompileOptions;
import org.joor.Reflect;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AeronicInterfaceHelperTest
{

    @Test
    public void shouldProcessClasses()
    {
        final TestAeronicAnnotationProcessor processor = new TestAeronicAnnotationProcessor();
        Reflect.compile(
            "io.aeronic.TestAeronicClass",
            """
                package io.aeronic;
                       
                import java.util.List;
                import io.aeronic.gen.AeronicInterfaceHelperTest.TestEncodable;
                                        
                @Aeronic
                public interface TestAeronicClass {
                    void doSomething(List<TestEncodable> list);
                }    
                """,
            new CompileOptions().processors(processor)
        );

        final List<MethodInfo> methodInfoList = processor.aeronicInterfaceHelper.getMethodInfoFor("TestAeronicClass");
        final MethodInfo doSomethingMethod = methodInfoList.get(0);
        final List<ParameterInfo> parameters = doSomethingMethod.getParameters();
        final ParameterInfo listParameter = parameters.get(0);

        assertThat(parameters).hasSize(1);
        assertEquals("io.aeronic.gen.AeronicInterfaceHelperTest.TestEncodable", listParameter.getGenericParameters().get(0));

        final SubscriberInvokerGenerator subscriberInvokerGenerator = new SubscriberInvokerGenerator();
        final PublisherGenerator publisherGenerator = new PublisherGenerator();

        final String generatedSubSrc = subscriberInvokerGenerator.generate("io.aeronic", "TestAeronicClass", methodInfoList);
        final String generatedPubSrc = publisherGenerator.generate("io.aeronic", "TestAeronicClass", methodInfoList);


        System.out.println(generatedSubSrc);
        System.out.println(generatedPubSrc);
    }

    @SupportedAnnotationTypes("io.aeronic.Aeronic")
    @SupportedSourceVersion(SourceVersion.RELEASE_17)
    @AutoService(Processor.class)
    private static class TestAeronicAnnotationProcessor extends AbstractProcessor
    {

        public final AeronicInterfaceHelper aeronicInterfaceHelper = new AeronicInterfaceHelper();

        @Override
        public boolean process(final Set<? extends TypeElement> set, final RoundEnvironment roundEnvironment)
        {
            aeronicInterfaceHelper.processEnvironment(roundEnvironment);
            return true;
        }
    }

    public static final class TestEncodable implements Encodable
    {

        private final int anInt;

        public TestEncodable(final int anInt)
        {
            this.anInt = anInt;
        }

        @Override
        public void encode(final BufferEncoder bufferEncoder)
        {
            bufferEncoder.encode(anInt);
        }

        @DecodedBy
        public static TestEncodable decode(final BufferDecoder bufferDecoder)
        {
            final int anInt = bufferDecoder.decodeInt();
            return new TestEncodable(anInt);
        }
    }
}