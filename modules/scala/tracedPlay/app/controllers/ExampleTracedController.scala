package controllers

import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExampleTracedController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext)
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
      }
    }
  }

}
