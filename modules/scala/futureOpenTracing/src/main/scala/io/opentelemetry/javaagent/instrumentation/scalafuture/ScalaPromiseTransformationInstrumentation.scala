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
import scala.annotation.static

// 2.13 and above
object ScalaPromiseTransformationInstrumentation {
  val TRANSFORMATION_CLASS_NAME = "scala.concurrent.impl.Promise$Transformation";

  class ConstructorAdvice

  object ConstructorAdvice {

    @static
    @Advice.OnMethodExit(suppress = classOf[Throwable], inline = false)
    def onConstruct(@Advice.This thiz: Runnable): Unit = {
      val context: io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Context =
        Java8BytecodeBridge.currentContext()

      if (ExecutorAdviceHelper.shouldPropagateContext(context, thiz)) {
        ExecutorAdviceHelper.attachContextToTask(context, VirtualFields.RUNNABLE_PROPAGATED_CONTEXT, thiz);
      }
    }
  }

  class RunAdvice
  object RunAdvice {

    @static
    @Advice.OnMethodEnter(suppress = classOf[Throwable], inline = false)
    @Nullable
    def enter(@Advice.This thiz: Runnable): Scope = {
      TaskAdviceHelper.makePropagatedContextCurrent(VirtualFields.RUNNABLE_PROPAGATED_CONTEXT, thiz);
    }

    @static
    @Advice.OnMethodExit(onThrowable = classOf[Throwable], suppress = classOf[Throwable], inline = false)
    def exit(@Advice.This thiz: Runnable, @Advice.Enter @Nullable scope: Scope): Unit =
      if (scope != null) {
        scope.close()
      }
  }
}

class ScalaPromiseTransformationInstrumentation extends TypeInstrumentation {

  override def classLoaderOptimization(): ElementMatcher[ClassLoader] = {
    AgentElementMatchers.hasClassesNamed(ScalaPromiseTransformationInstrumentation.TRANSFORMATION_CLASS_NAME)
  };

  override def typeMatcher(): ElementMatcher[TypeDescription] = {

    ElementMatchers.named(ScalaPromiseTransformationInstrumentation.TRANSFORMATION_CLASS_NAME)
  }

  override def transform(transformer: TypeTransformer): Unit = {
    transformer.applyAdviceToMethod(
      ElementMatchers.isConstructor(),
      classOf[ScalaPromiseTransformationInstrumentation.ConstructorAdvice].getName
    )

    transformer.applyAdviceToMethod(
      ElementMatchers.named("run"),
      classOf[ScalaPromiseTransformationInstrumentation.RunAdvice].getName
    )
  }
}
