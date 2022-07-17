package io.aeronic.gen;

import com.google.auto.service.AutoService;
import org.joor.CompileOptions;
import org.joor.Reflect;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Set;

import static io.aeronic.gen.Samples.SAMPLE_PUBLISHER;
import static io.aeronic.gen.Samples.SAMPLE_SUBSCRIBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CodeGenerationTest
{

    @Test
    public void shouldGenerateCode()
    {
        final TestAeronicAnnotationProcessor processor = new TestAeronicAnnotationProcessor();
        Reflect.compile(
            "io.aeronic.TestEvents",
            """
                package io.aeronic;
                                   
                import java.util.List;
                import io.aeronic.codec.SimpleImpl;
                                                    
                @Aeronic
                public interface TestEvents
                {
                    void onEvent(
                        long aLong,
                        int intValue,
                        float floatValue,
                        double doubleValue,
                        byte byteValue,
                        char charValue,
                        SimpleImpl simpleImpl,
                        String stringValue,
                        long[] longs,
                        int[] ints,
                        float[] floats,
                        double[] doubles,
                        byte[] bytes,
                        char[] chars,
                        SimpleImpl[] simples,
                        MyEnum myEnum,
                        List<SimpleImpl> simpleList
                    );

                    void onTimer(long timestamp);
                } 
                """,
            new CompileOptions().processors(processor)
        );

        final List<MethodInfo> methodInfoList = processor.aeronicInterfaceHelper.getMethodInfoFor("TestEvents");
        assertThat(methodInfoList).hasSize(2);

        final SubscriberInvokerGenerator subscriberInvokerGenerator = new SubscriberInvokerGenerator();
        final PublisherGenerator publisherGenerator = new PublisherGenerator();

        final String generatedSubSrc = subscriberInvokerGenerator.generate("io.aeronic", "TestEvents", methodInfoList);
        final String generatedPubSrc = publisherGenerator.generate("io.aeronic", "TestEvents", methodInfoList);

        assertEquals(SAMPLE_SUBSCRIBER, generatedSubSrc);
        assertEquals(SAMPLE_PUBLISHER, generatedPubSrc);
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
}