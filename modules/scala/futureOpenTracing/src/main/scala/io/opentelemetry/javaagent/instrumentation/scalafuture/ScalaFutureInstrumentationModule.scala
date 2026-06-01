package io.opentelemetry.javaagent.instrumentation.scalafuture

import io.opentelemetry.javaagent.extension.instrumentation.{InstrumentationModule, TypeInstrumentation}

import java.util

class ScalaFutureInstrumentationModule
    extends InstrumentationModule("scala-future", "scala-future-2.10", "scala-concurrent") {

  override def typeInstrumentations(): util.List[TypeInstrumentation] = {
    // uses java as scala convertors are not available and cause wierd no-arg constructor errors.
    util.Arrays.asList(
      new ScalaCallbackRunnableInstrumentation,
      new ScalaPromiseTransformationInstrumentation
    )
  }
}
