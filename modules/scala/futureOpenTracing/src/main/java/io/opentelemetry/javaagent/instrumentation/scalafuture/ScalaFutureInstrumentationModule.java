package io.opentelemetry.javaagent.instrumentation.scalafuture;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class ScalaFutureInstrumentationModule extends InstrumentationModule {

    public ScalaFutureInstrumentationModule() {
        super("scala-future", "scala-future-2.10", "scala-concurrent");

        System.out.println("carsarecool");
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return asList(
                new ScalaCallbackRunnableInstrumentation(),
                new ScalaPromiseTransformationInstrumentation());
    }
}