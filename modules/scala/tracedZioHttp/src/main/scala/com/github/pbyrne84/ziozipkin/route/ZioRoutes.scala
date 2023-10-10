package com.github.pbyrne84.ziozipkin.route

import com.github.pbyrne84.ziozipkin.client.TracingClient
import com.github.pbyrne84.ziozipkin.tracing.{B3HTTPResponseTracing, B3Tracing, TracedRequest}
import zio.{RuntimeFlags, Scope, ULayer, ZIO, ZLayer}
import zio.http.{Client, Headers, Http, Method, Request, Response, Root}
import zio.http._
import zio.telemetry.opentelemetry.Tracing

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

  val routes = Http.collectZIO[Request] {
//     int() is hiding in the RouteDecoderModule, could not find it in any examples but I may be blind
//    case req @ Method.GET -> Root / "proxy" / int(id) =>
//      callTracedService(req, id)
//
//    case Method.GET -> Root / "delete1" =>
//      Builds.PersonServiceBuild.personServiceBuild
//        .flatMap(_.deletePeople().map((deleteCount: Long) => Response.text(deleteCount.toString)))
//
//    case Method.GET -> Root / "delete2" =>
//      PersonRepo
//        .deletePeople()
//        .map((deleteCount: Long) => Response.text(deleteCount.toString))
//        .provideLayer(Builds.PersonRepoBuild.personRepoMake)
//
//    case Method.GET -> Root / "delete3" =>
//      personRepo
//        .deletePeople()
//        .map((deleteCount: Long) => Response.text(deleteCount.toString))

    case req @ Method.GET -> Root / "moo" =>
      ZIO.succeed(Response.text("bnana"))

    case req @ Method.GET -> Root / "test" =>
      traced("zio-test-route", req) {
        ZIO.attempt(Response.text("content"))
      }.tapError(error => ZIO.succeed(println("bananananaananan")))

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
