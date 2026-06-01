package io.opentelemetry.javaagent.instrumentation.scalafuture;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.instrumentation.scalafuture.VirtualFields.RUNNABLE_PROPAGATED_CONTEXT;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.shaded.io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.bootstrap.executors.ExecutorAdviceHelper;
import io.opentelemetry.javaagent.bootstrap.executors.TaskAdviceHelper;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import javax.annotation.Nullable;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * Instruments {@code scala.concurrent.impl.Promise$Transformation} (Scala 2.13+), which replaced
 * {@code CallbackRunnable} as the {@link Runnable} backing all Future callbacks.
 */
class ScalaPromiseTransformationInstrumentation implements TypeInstrumentation {

    static final String TRANSFORMATION_CLASS_NAME =
            "scala.concurrent.impl.Promise$Transformation";

    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
        return hasClassesNamed(TRANSFORMATION_CLASS_NAME);
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named(TRANSFORMATION_CLASS_NAME);
    }

    @Override
    public void transform(TypeTransformer transformer) {
        System.out.println("bananana");

        transformer.applyAdviceToMethod(
                isConstructor(), getClass().getName() + "$ConstructorAdvice");
        transformer.applyAdviceToMethod(
                named("run"), getClass().getName() + "$RunAdvice");
    }

    @SuppressWarnings("unused")
    public static class ConstructorAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class, inline = false)
        public static void onConstruct(@Advice.This Runnable thiz) {
            Context context = Java8BytecodeBridge.currentContext();
            if (ExecutorAdviceHelper.shouldPropagateContext(context, thiz)) {
                ExecutorAdviceHelper.attachContextToTask(context, RUNNABLE_PROPAGATED_CONTEXT, thiz);
            }
        }
    }

    @SuppressWarnings("unused")
    public static class RunAdvice {

        @Nullable
        @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
        public static Scope enter(@Advice.This Runnable thiz) {
            System.out.println("bananana");
            return TaskAdviceHelper.makePropagatedContextCurrent(RUNNABLE_PROPAGATED_CONTEXT, thiz);
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class, inline = false)
        public static void exit(
                @Advice.This Runnable thiz, @Advice.Enter @Nullable Scope scope) {
            System.out.println("bananana");
            if (scope != null) {
                scope.close();
            }

        }
    }
}