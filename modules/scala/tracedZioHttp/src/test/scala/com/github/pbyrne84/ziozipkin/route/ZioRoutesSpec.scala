package com.github.pbyrne84.ziozipkin.route
import com.github.pbyrne84.ziozipkin.tracing.B3HTTPResponseTracing
import zio.http.{Path, Request, URL}
import zio.logging.backend.SLF4J
import zio.telemetry.opentelemetry.Tracing
import zio.test._
import zio.{Scope, ZIO}
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault}

object ZioRoutesSpec extends ZIOSpecDefault {

  private val loggingLayer = zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private def callService(
      request: Request
  ) = {
    ZIO
      .serviceWithZIO[ZioRoutes](_.routes.runZIO(request))
  }

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("when calling the service")(
      test("then it shown return 200") {
        val request = Request.get(url = URL.empty.copy(path = Path.decode("/test")))

        val value = zio.Runtime.removeDefaultLoggers >>> loggingLayer
        callService(request).map { result =>
          assertTrue(
            result.status.code == 200
          )
        }

      }
    ).provide(B3HTTPResponseTracing.layer,
              ZioRoutes.routesLayer,
              Tracing.live,
              NonExportingTracer.live,
              zio.Runtime.removeDefaultLoggers >>> loggingLayer)

}
