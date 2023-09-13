package logging

import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import kamon.context.Context

object LoggingLayout {

  val ActionKey: Context.Key[String] = Context.key[String]("action", "undefined")
  val ParentSpanKey: Context.Key[String] = Context.key[String]("kamonParentSpanId", "undefined")
  val CurrentRequestKey: Context.Key[Option[HttpRequest]] = Context.key[Option[HttpRequest]]("request", None)
  val EntityKey: Context.Key[Option[HttpEntity.Strict]] =
    Context.key[Option[HttpEntity.Strict]]("entity", None)

}
