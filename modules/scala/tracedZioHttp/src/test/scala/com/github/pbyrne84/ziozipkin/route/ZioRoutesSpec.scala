package com.github.pbyrne84.ziozipkin.route
import zio.http.{Client, Headers, Request, Response, Server, Status, TestServer, URL}
import zio.test._
import zio.{Scope, ZIO}

object ZioRoutesSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] = {
    def runRequest(
        headers: Headers
    ): ZIO[TestServer & ZioRoutes & Client, Throwable, Response] = {
      for {
        client <- ZIO.service[zio.http.Client]
        routes <- ZIO.service[ZioRoutes]
        port <- ZIO.serviceWithZIO[Server](_.port)
        request = Request
          .get(url = URL.root.port(port).path("/test"))
          .addHeaders(headers)
        _ <- TestServer.addRoutes(routes.routes)
        response <- client.batched(request)
      } yield response
    }

    suite("when calling the service")(
      test(
        "then it should generate the trace id and span id in the response headers when they were not passed in the request"
      ) {

        {
          for {
            response <- runRequest(Headers.empty)
            body <- response.body.asString
            maybeTraceId = response.headers.get("x-b3-traceid")
            maybeSpanId = response.headers.get("x-b3-spanid")
          } yield assertTrue(
            response.status == Status.Ok,
            body == "content",
            maybeTraceId.isDefined,
            maybeSpanId.isDefined
          )

        }
      },
      test(
        "then it should return the same trace id but different span id in the response headers when tracing headers are passed"
      ) {
        ZIO.scoped {
          for {
            response <- runRequest(
              Headers(("x-b3-traceid", "130f400f1325f59fc914421f0058aa41"), ("x-b3-spanid", "79479ced44b1bf73"))
            )
            body <- response.body.asString
            maybeTraceId = response.headers.get("x-b3-traceid")
            maybeSpanId = response.headers.get("x-b3-spanid")
          } yield assertTrue(
            response.status == Status.Ok,
            body == "content",
            maybeTraceId.contains("130f400f1325f59fc914421f0058aa41"),
            maybeSpanId.isDefined,
            !maybeSpanId.contains("79479ced44b1bf73")
          )
        }
      }
    ).provide(
      ZioRoutes.routesLayer,
      zio.http.Client.default,
      TestServer.default
    )
  }

}
