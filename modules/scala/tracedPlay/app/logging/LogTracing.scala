package logging

import com.typesafe.scalalogging.StrictLogging
import kamon.Kamon

trait LogTracing extends StrictLogging {

  opaque type LogAction = String
  opaque type ControllerLogAction = String

  private implicit class ActionMessageOps(text: String) {
    def asAction: LogAction = text
    def asControllerAction: ControllerLogAction = text
  }

  implicit class LogActionOps(logAction: LogAction) {
    inline def value: String = logAction
  }

  implicit class ControllerLogActionOps(controllerLogAction: ControllerLogAction) {
    inline def value: String = controllerLogAction
  }

  private[logging] val prefix = "tp-"

  object controllerActionMarker {
    private[LogTracing] val controllerPrefix = s"${prefix}controller-"

    val showHomePage: ControllerLogAction =
      s"${controllerPrefix}show-homepage"

    val testRequestCallBack: ControllerLogAction =
      s"${controllerPrefix}test-request-call-back"

  }

  object actionMarker {

    val childAction: LogAction =
      s"${prefix}child-action".asAction
  }

  def wrapActionWithLogging[A](action: LogAction)(call: => A): A = {
    Kamon.runWithContextEntry(LoggingLayout.ActionKey, action) {
      call
    }
  }

}
