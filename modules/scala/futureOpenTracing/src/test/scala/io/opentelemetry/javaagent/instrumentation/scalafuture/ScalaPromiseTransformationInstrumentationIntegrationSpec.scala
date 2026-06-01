package io.opentelemetry.javaagent.instrumentation.scalafuture

import io.opentelemetry.api.trace.Tracer
import org.scalactic.source.Position
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.matching.Regex

object PromiseChildMain {

  import io.opentelemetry.api.GlobalOpenTelemetry
  import io.opentelemetry.exporter.logging.LoggingSpanExporter
  import io.opentelemetry.sdk.OpenTelemetrySdk
  import io.opentelemetry.sdk.resources.Resource
  import io.opentelemetry.sdk.trace.SdkTracerProvider
  import io.opentelemetry.sdk.trace.`export`.BatchSpanProcessor
  import io.opentelemetry.semconv.ServiceAttributes

  given ExecutionContext = ExecutionContext.global

  lazy val tracer: Tracer = locally {
    val serviceResource = Resource.getDefault.merge(
      Resource.builder
        .put(ServiceAttributes.SERVICE_NAME, "play-app")
        .put(ServiceAttributes.SERVICE_VERSION, "1.0.0")
        .build
    )

    val tracerProvider: SdkTracerProvider = SdkTracerProvider.builder
      .addSpanProcessor(BatchSpanProcessor.builder(LoggingSpanExporter.create()).build)
      .setResource(serviceResource)
      .build

    val openTelemetry: OpenTelemetrySdk =
      OpenTelemetrySdk.builder.setTracerProvider(tracerProvider).buildAndRegisterGlobal

    val tracer = GlobalOpenTelemetry.getTracer("application")
    tracer
  }

  def main(args: Array[String]): Unit = {
    val name = "getProductDetails"

    val s = createSpan(name)
      .startSpan()

    s.makeCurrent()

    println("span0TraceId:" + s.getSpanContext.getTraceId)

    val a = for {
      _ <- span1
      result <- span2.flatMap(_ => span3)
    } yield result

    val result = Await.result(a, 10.seconds)
    // Marker line the parent test will assert on.
    println(s"CHILD_RESULT=$result")
  }

  private def createSpan(name: String) = {
    tracer
      .spanBuilder(name)
      .setAttribute(s"$name.id", s"$name.value")
  }

  private def span1: Future[String] = {
    Future {
      val span = createSpan("span1").startSpan()
      span.makeCurrent()
      println("span1TraceId:" + span.getSpanContext.getTraceId)
      "span1-result"
    }
  }

  private def span2: Future[String] = {
    Future {
      val span = createSpan("span2").startSpan()
      span.makeCurrent()
      println("span2TraceId:" + span.getSpanContext.getTraceId)
      "span2-result"
    }
  }

  private def span3: Future[String] = {
    Future {
      val span = createSpan("span3").startSpan()
      span.makeCurrent()
      println("span3TraceId:" + span.getSpanContext.getTraceId)
      "span3-result"
    }
  }

}

class ScalaPromiseTransformationInstrumentationIntegrationSpec extends AnyFreeSpecLike with Matchers {

  "ScalaPromiseTransformationInstrumentation (in a forked JVM with the OTel agent)" - {

    def getTraceId(output: String, index: Int)(implicit position: Position): String = {
      val regex: Regex = s"(?s).*span${index}TraceId:((\\d|[a-z]){32})(.*)".r

      output match {
        case regex(_, traceId, _) => traceId
        case text =>
          fail(s"regex $regex was not found in\n\n $text")
      }

    }

    "instruments Promise$Transformation and the child program completes" in {

      val agentJar = sysPropOrFail("otel.agent.jar")
      val extensionJar = sysPropOrFail("otel.extension.jar")
      val javaHome = sysPropOrFail("otel.java.home")

      val javaBin = Paths.get(javaHome, "bin", "java").toString
      // Reuse the parent test's classpath so the child can load PromiseChildMain
      val classpath = System.getProperty("java.class.path")

      val cmd = List(
        javaBin,
        "-Xmx256m",
        "-Dotel.javaagent.debug=true",
        "-Dotel.java.global-autoconfigure.enabled=true",
        s"-javaagent:$agentJar",
        s"-Dotel.javaagent.extensions=$extensionJar",
        // turn off exporters so the child doesn't try to talk to a collector
        "-Dotel.traces.exporter=none",
        "-Dotel.metrics.exporter=none",
        "-Dotel.logs.exporter=none",
        "-cp",
        classpath,
        classOf[PromiseChildMain.type].getName.stripSuffix("$")
      )

      info(s"Launching: ${cmd.mkString(" ")}")

      val process = new ProcessBuilder(cmd*)
        .redirectErrorStream(true)
        .start()

      // Don't pipe in any input; just read everything the child prints.
      process.getOutputStream.close()

      val output =
        new String(process.getInputStream.readAllBytes(), StandardCharsets.UTF_8)
      val exit = process.waitFor()

      println(output)

      withClue(s"child exited $exit, output was:\n$output\n") {
        exit shouldBe 0
      }

      val tractId0 = getTraceId(output, 0)
      val tractId1 = getTraceId(output, 1)
      val tractId2 = getTraceId(output, 2)
      val tractId3 = getTraceId(output, 3)

      withClue("tractId1 should be the same") {
        tractId1 shouldBe tractId0
      }

      withClue("tractId2 should be the same") {
        tractId2 shouldBe tractId0
      }

      withClue("tractId3 should be the same") {
        tractId3 shouldBe tractId0
      }

      // 3. The OTel agent picked our extension up
      output should include regex
        "(?i)(loading extension|installing extension|ScalaFutureInstrumentationModule)"
    }
  }

  private def sysPropOrFail(name: String): String =
    Option(System.getProperty(name)).getOrElse(
      fail(s"system property '$name' not set. Did the build set Test / javaOptions?")
    )
}
