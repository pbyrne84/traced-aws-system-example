//package io.opentelemetry.javaagent.instrumentation.scalafuture;
//
//import static java.util.Arrays.asList;
//
//import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
//import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
//import java.util.List;
//
//public class ScalaFutureInstrumentationModule extends InstrumentationModule {
//
//    public ScalaFutureInstrumentationModule() {
//        super("scala-future", "scala-future-2.10", "scala-concurrent");
//    }
//
//    @Override
//    public List<TypeInstrumentation> typeInstrumentations() {
//        return asList(
//                new ScalaCallbackRunnableInstrumentation(),
//                new ScalaPromiseTransformationInstrumentation());
//    }
//}