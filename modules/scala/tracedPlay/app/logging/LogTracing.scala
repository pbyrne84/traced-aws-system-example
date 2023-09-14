package logging

import com.typesafe.scalalogging.StrictLogging
import kamon.Kamon
import shapeless.tag.@@

trait LogTracing extends StrictLogging {

  trait ActionTag
  trait ControllerActionTag

  type LogAction = String @@ ActionTag
  type ControllerLogAction = String @@ ControllerActionTag

  import shapeless.tag

  private implicit class ActionMessageOps(text: String) {
    def asAction: String @@ ActionTag = tag[ActionTag](text)
    def asControllerAction: String @@ ControllerActionTag = tag[ControllerActionTag](text)
  }

  private[logging] val prefix = "tp-"

  object controllerActionMarker {
    private[LogTracing] val controllerPrefix = s"${prefix}controller-"

    val showHomePage: ControllerLogAction =
      s"${controllerPrefix}show-homepage".asControllerAction

    val testRequestCallBack: ControllerLogAction =
      s"${controllerPrefix}test-request-call-back".asControllerAction

  }

  object actionMarker {

    val childAction: String @@ ActionTag =
      s"${prefix}child-action".asAction
  }

  def wrapActionWithLogging[A](action: LogAction)(call: => A): A = {
    Kamon.runWithContextEntry(LoggingLayout.ActionKey, action) {
      call
    }
  }

}
