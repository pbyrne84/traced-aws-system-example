package io.opentelemetry.javaagent.instrumentation.scalafuture

import io.opentelemetry.javaagent.bootstrap.executors.PropagatedContext
import io.opentelemetry.javaagent.shaded.instrumentation.api.util.VirtualField

object VirtualFields {

  val RUNNABLE_PROPAGATED_CONTEXT: VirtualField[Runnable, PropagatedContext] =
    VirtualField.find(classOf[Runnable], classOf[PropagatedContext])
}
