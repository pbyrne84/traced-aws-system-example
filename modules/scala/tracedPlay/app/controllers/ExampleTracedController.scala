package controllers

import kamon.Kamon
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExampleTracedController @Inject() (ws: WSClient, cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with TraceInitialisation {

  Kamon.init()

  def testSuccess: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    traceAsync(controllerActionMarker.showHomePage, request) {
      childCall.flatMap(_ => Future.successful(Ok(Kamon.currentSpan().trace.toString)))
    }
  }

  private def childCall: Future[Boolean] = {
    wrapActionWithLogging(actionMarker.childAction) {
      Future {
        logger.info("play-banana")
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
