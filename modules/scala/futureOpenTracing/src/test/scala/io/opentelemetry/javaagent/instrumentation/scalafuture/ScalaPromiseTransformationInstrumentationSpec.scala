package io.opentelemetry.javaagent.instrumentation.scalafuture

import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.matcher.ElementMatcher
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers

import java.util.function
import scala.collection.mutable.ListBuffer

class ScalaPromiseTransformationInstrumentationSpec extends AnyFreeSpecLike with Matchers {

  private val transformationClassName =
    ScalaPromiseTransformationInstrumentation.TRANSFORMATION_CLASS_NAME

  "ScalaPromiseTransformationInstrumentation" - {

    "should only match scala Promise Transformation classes" in {
      val instrumentation = new ScalaPromiseTransformationInstrumentation
      val transformationClass = Class.forName(transformationClassName)

      // This is a private class so we need to use reflection to circumvent
      val className = "scala.concurrent.impl.Promise$Transformation"
      val expectedClass = Class.forName(className)

      transformationClass shouldBe expectedClass

    }

    "should apply constructor and run advice" in {
      val instrumentation = new ScalaPromiseTransformationInstrumentation
      val appliedAdvice = ListBuffer.empty[(ElementMatcher[? >: MethodDescription], String)]

      val transformer = new TypeTransformer {
        override def applyAdviceToMethod(
            methodMatcher: ElementMatcher[? >: MethodDescription],
            mappingCustomizer: function.Function[Advice.WithCustomMapping, Advice.WithCustomMapping],
            adviceClassName: String
        ): Unit =
          appliedAdvice += methodMatcher -> adviceClassName

        override def applyTransformer(transformer: AgentBuilder.Transformer): Unit = ???
      }

      instrumentation.transform(transformer)

      appliedAdvice.map(_._2).toList shouldBe List(
        classOf[ScalaPromiseTransformationInstrumentation].getName + "$ConstructorAdvice",
        classOf[ScalaPromiseTransformationInstrumentation].getName + "$RunAdvice"
      )

      val transformationClass = Class.forName(transformationClassName)

      val constructorDescription =
        new MethodDescription.ForLoadedConstructor(
          transformationClass.getDeclaredConstructors.head
        )

      val runDescription =
        new MethodDescription.ForLoadedMethod(
          transformationClass.getDeclaredMethod("run")
        )

      val constructorMatcher = appliedAdvice.head._1
      val runMatcher = appliedAdvice(1)._1

      constructorMatcher.matches(constructorDescription) shouldBe true
      constructorMatcher.matches(runDescription) shouldBe false

      runMatcher.matches(runDescription) shouldBe true
      runMatcher.matches(constructorDescription) shouldBe false
    }
  }
}
