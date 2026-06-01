package controllers

import io.opentelemetry.api.trace.Tracer
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExampleTracedController @Inject() (ws: WSClient, cc: ControllerComponents, tracer: Tracer)(implicit
    ec: ExecutionContext
) extends AbstractController(cc)
    with TraceInitialisation {

  def testSuccess: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    traceAsync(controllerActionMarker.showHomePage, request) {

      val s = tracer
        .spanBuilder("getProductDetails")
        .setAttribute("product.id", "sss")
        .startSpan();

      s.makeCurrent()

      childCall.flatMap { _ =>
        logger.info("play-banana2 " + s.getSpanContext.getTraceId)
        s.end()
        Future.successful(Ok(s.getSpanContext.getTraceId + " " + tracer.isEnabled))
      }
    }
  }

  private def childCall: Future[Boolean] = {
    wrapActionWithLogging(actionMarker.childAction) {

      Future {

        val s = tracer
          .spanBuilder("getProductDetails")
          .setAttribute("product.id", "sss")
          .startSpan();

        logger.info("play-banana1 " + s.getSpanContext.getTraceId)
      }.flatMap { _ =>
        // val eventualResponse: Future[WSResponse] = ws.url("http://localhost:8080/test-success").get()

        Future.successful(true).map(_ => true)
      }
    }

  }

  def testFailure: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    traceAsync(controllerActionMarker.testRequestCallBack, request) {

      logger.info("child test call")

      val eventualResponse: Future[WSResponse] = ws.url("http://localhost:8080/test-fail").get()
      eventualResponse.map { result =>
        if (result.status >= 500) {
          InternalServerError(s"moooo ${result.status}")
        } else {
          Ok(s"moooo ${result.status}")
        }

      }
    }
  }

}
