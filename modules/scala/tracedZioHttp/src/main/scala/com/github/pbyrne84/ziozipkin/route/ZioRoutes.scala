package com.github.pbyrne84.ziozipkin.route

import com.github.pbyrne84.ziozipkin.client.TracingClient
import com.github.pbyrne84.ziozipkin.tracing.{B3HTTPResponseTracing, TracedRequest}
import zio.http._
import zio.telemetry.opentelemetry.Tracing
import zio.{RuntimeFlags, Scope, ULayer, ZIO, ZLayer}

object RoutesBuild {

  lazy val routesBuild: ZIO[Any with Scope, Throwable, ZioRoutes] = ZLayer
    .make[ZioRoutes](
      ZioRoutes.routesLayer
    )
    .build
    .map(_.get[ZioRoutes])
}

object ZioRoutes {
  val routesLayer: ULayer[ZioRoutes] = ZLayer.succeed(new ZioRoutes)

}

class ZioRoutes extends TracedRequest {

  val routes: Routes[Tracing with B3HTTPResponseTracing, Response] = {
    Routes(
      Method.GET / "moo" -> handler(ZIO.succeed(Response.text("banana"))),
      Method.GET / "greet" -> handler { (req: Request) =>
        val name = req.queryOrElse[String]("name", "World")
        Response.text(s"Hello $name!")
      },
      Method.GET / "test" -> handler { (req: Request) =>
        traced("zio-test-route", req) {
          ZIO.succeed(Response.text("content"))
        }.mapError(a => Response.text(a.toString))
      }
    )
  }

  // TracingHttp in com.github.pbyrne84.zio2playground.http does all the add tracing stuff in theory.
  // it takes a Http.collectZIO[Request] compatible format of
  // routes: PartialFunction[Request, ZIO[R, E, Response]] and
  // just intercepts the request before the call to do the processing.
  private def callTracedService(req: Request, id: RuntimeFlags): ZIO[
    Tracing with B3HTTPResponseTracing with TracingClient with Client,
    Throwable,
    Response
  ] = {
    for {
      _ <- ZIO.logInfo(s"received a called traced service call with the id $id")
//          result <- ExternalApiService
//            .callApi(id)
//          content <- result.body.asString
      _ <- ZIO.logInfo(s"running the banana")

    } yield {
      Response.text("content")
    }
  }
}
