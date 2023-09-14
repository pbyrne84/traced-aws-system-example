package controllers

import akka.http.scaladsl.model.headers.RawHeader
import akka.util.ByteString
import kamon.Kamon
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, HttpMethod, HttpRequest}
import logging.{LogTracing, LoggingLayout}
import play.api.mvc.{AnyContent, MessagesRequest, Request, Result}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

//Adds a loggable trace per request with information about request
//Also adds the trace details to the response.
trait TraceInitialisation extends LogTracing {
  def trace[A](action: ControllerLogAction, request: Request[AnyContent])(call: => Result): Result = {
    Kamon.runWithContextEntry(LoggingLayout.ActionKey, action) {
      //make sure we have one action for dumb tasks
      logger.info(s"Running $action")
      addTraceHeadersToResult(call)
    }
  }

  private def addTraceHeadersToResult(result: Result) = {
    result
  }

  def traceAsync[A](
      action: ControllerLogAction,
      request: Request[AnyContent]
  )(call: => Future[Result])(implicit ec: ExecutionContext): Future[Result] = {

    val (httpRequest: HttpRequest, maybeStrictEntity) = createRequestEntry(request)
    val span = Kamon.spanBuilder(action).start()

    Kamon.runWithSpan(span) {
      Kamon.runWithContextEntry(LoggingLayout.ParentSpanKey, Kamon.currentSpan().parentId.string) {
        Kamon.runWithContextEntry(LoggingLayout.ActionKey, action) {
          Kamon.runWithContextEntry(LoggingLayout.CurrentRequestKey, Some(httpRequest)) {
            Kamon.runWithContextEntry(LoggingLayout.EntityKey, maybeStrictEntity) {
              //stops multiple calls firing off as detaches from call by name =>
              val eventualResultFromCall = call
              logger.info("start of processing")
              eventualResultFromCall.onComplete {
                case Failure(exception) =>
                  logger.error(s"Failed processing request: $request", exception)
                case Success(_: Result) =>
                  logger.info(s"Successfully processed request without exception: $request")
              }

              eventualResultFromCall
            }
          }
        }
      }
    }
  }

  private def createRequestEntry(request: Request[AnyContent]): (HttpRequest, Option[HttpEntity.Strict]) = {
    val maybeStrictEntity = if (request.hasBody) {
      val maybeJson = request.body.asFormUrlEncoded.map { entries: Map[String, Seq[String]] =>
        import io.circe.syntax._
        entries.asJson

      }

      maybeJson
        .map(json => HttpEntity.Strict(ContentTypes.`application/json`, ByteString.apply(json.spaces2)))

    } else {
      None
    }

    val headers = request.headers.headers.map(header => RawHeader(header._1, header._2))
    (HttpRequest(method = HttpMethod.custom(request.method.toUpperCase), uri = request.uri, headers = headers),
     maybeStrictEntity)

  }
}
