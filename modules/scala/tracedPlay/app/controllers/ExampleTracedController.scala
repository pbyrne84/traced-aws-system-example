package controllers

import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExampleTracedController @Inject()(ws: WSClient, cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with TraceInitialisation {

  def index: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    traceAsync(controllerActionMarker.showHomePage, request) {
      childCall.flatMap(_ => Future.successful(Ok("It works!")))
    }
  }

  private def childCall: Future[Unit] = {
    wrapActionWithLogging(actionMarker.childAction) {
      Future {
        logger.info("banana")
      }.map { _ =>
        val eventualResponse: Future[WSResponse] = ws.url("http://localhost:9000/test").get()
        eventualResponse
      }
    }

  }

  def test: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    traceAsync(controllerActionMarker.testRequestCallBack, request) {
      logger.info("child test call")
      Future.successful(Ok("moooo"))
    }
  }

}
