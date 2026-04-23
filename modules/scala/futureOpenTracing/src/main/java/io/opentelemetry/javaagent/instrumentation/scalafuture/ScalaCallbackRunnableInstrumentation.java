package io.opentelemetry.javaagent.instrumentation.scalafuture;


import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.bootstrap.executors.ExecutorAdviceHelper;
import io.opentelemetry.javaagent.bootstrap.executors.PropagatedContext;
import io.opentelemetry.javaagent.bootstrap.executors.TaskAdviceHelper;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Scope;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import javax.annotation.Nullable;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Instruments {@code scala.concurrent.impl.CallbackRunnable} (Scala 2.10 - 2.12), which is the
 * {@link Runnable} that backs every Scala {@code Future} callback ({@code onComplete}, {@code map},
 * {@code flatMap}, {@code transform}, ...).
 *
 * <p>Note: There are several Runnable implementations involved in Scala futures. JVM, Akka and
 * Scala fork-join pools are handled elsewhere; this class explicitly captures the calling context
 * at the moment a Future callback is registered, and re-installs it when that callback runs on the
 * underlying {@code ExecutionContext}.
 */
class ScalaCallbackRunnableInstrumentation implements TypeInstrumentation {

    static final String CALLBACK_RUNNABLE_CLASS_NAME = "scala.concurrent.impl.CallbackRunnable";

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
        return hasClassesNamed(CALLBACK_RUNNABLE_CLASS_NAME);
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named(CALLBACK_RUNNABLE_CLASS_NAME);
    }

    @Override
    public void transform(TypeTransformer transformer) {
        // Capture the calling context when the callback is constructed (i.e. registered).
        transformer.applyAdviceToMethod(
                isConstructor(), getClass().getName() + "$ConstructorAdvice");

        // Install the captured context for the duration of run().
        transformer.applyAdviceToMethod(
                named("run"), getClass().getName() + "$RunAdvice");
    }

    @SuppressWarnings("unused")
    public static class ConstructorAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class, inline = false)
        public static void onConstruct(@Advice.This Runnable thiz) {
            io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Context context = Java8BytecodeBridge.currentContext();
            if (ExecutorAdviceHelper.shouldPropagateContext(context, thiz)) {
                ExecutorAdviceHelper.attachContextToTask(context, VirtualFields.RUNNABLE_PROPAGATED_CONTEXT, thiz);
            }
        }
    }

    @SuppressWarnings("unused")
    public static class RunAdvice {

        @Nullable
        @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
        public static Scope enter(@Advice.This Runnable thiz) {
            return TaskAdviceHelper.makePropagatedContextCurrent(VirtualFields.RUNNABLE_PROPAGATED_CONTEXT, thiz);
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class, inline = false)
        public static void exit(
                @Advice.This Runnable thiz, @Advice.Enter @Nullable Scope scope) {
            if (scope != null) {
                scope.close();
            }
        }
    }
}