package io.opentelemetry.javaagent.instrumentation.scalafuture

import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge
import io.opentelemetry.javaagent.bootstrap.executors.{ExecutorAdviceHelper, TaskAdviceHelper}
import io.opentelemetry.javaagent.extension.instrumentation.{TypeInstrumentation, TypeTransformer}
import io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers
import io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Scope
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.`type`.TypeDescription
import net.bytebuddy.matcher.{ElementMatcher, ElementMatchers}

import javax.annotation.Nullable

//will need to fixed like ScalaPromiseTransformationInstrumentation using static, etc. in the companion objects
//if we really want to support below 2.13,
object ScalaCallbackRunnableInstrumentation {
  val CALLBACK_RUNNABLE_CLASS_NAME = "scala.concurrent.impl.CallbackRunnable"

  object ConstructorAdvice {

    @Advice.OnMethodExit(suppress = classOf[Throwable], inline = false)
    def onConstruct(@Advice.This thiz: Runnable): Unit = {
      val context: io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Context =
        Java8BytecodeBridge.currentContext()

      if (ExecutorAdviceHelper.shouldPropagateContext(context, thiz)) {
        ExecutorAdviceHelper.attachContextToTask(context, VirtualFields.RUNNABLE_PROPAGATED_CONTEXT, thiz);
      }
    }
  }

  object RunAdvice {

    def enter(@Advice.This thiz: Runnable): Scope = {
      TaskAdviceHelper.makePropagatedContextCurrent(VirtualFields.RUNNABLE_PROPAGATED_CONTEXT, thiz)
    }

    def exit(@Advice.This thiz: Runnable, @Advice.Enter @Nullable scope: Scope): Unit =
      if (scope != null) {
        scope.close()
      }
  }

}

class ScalaCallbackRunnableInstrumentation extends TypeInstrumentation {

  override def classLoaderOptimization(): ElementMatcher[ClassLoader] =
    AgentElementMatchers.hasClassesNamed(ScalaCallbackRunnableInstrumentation.CALLBACK_RUNNABLE_CLASS_NAME)

  override def typeMatcher(): ElementMatcher[TypeDescription] =
    ElementMatchers.named(ScalaCallbackRunnableInstrumentation.CALLBACK_RUNNABLE_CLASS_NAME)

  override def transform(transformer: TypeTransformer): Unit = {
    transformer.applyAdviceToMethod(
      ElementMatchers.isConstructor(),
      ScalaCallbackRunnableInstrumentation.ConstructorAdvice.getClass.getName
    )

    transformer.applyAdviceToMethod(
      ElementMatchers.named("run"),
      ScalaCallbackRunnableInstrumentation.RunAdvice.getClass.getName
    )
  }
}
