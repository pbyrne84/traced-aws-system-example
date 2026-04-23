package com.github.pbyrne84.akkahttpkamon

import com.typesafe.scalalogging.StrictLogging
import kamon.Kamon
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{HttpRequest, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.mdedetrich.pekko.http.support.CirceHttpSupport

import scala.concurrent.{ExecutionContextExecutor, Future}

object AkkHttpBoot extends StrictLogging with CirceHttpSupport {

  Kamon.init()

  private implicit val system: ActorSystem = ActorSystem()
  private implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  private val zioPort = 58479

  def main(args: Array[String]): Unit = {

    val port = 8080
    val bindingFuture = Http().newServerAt("localhost", port).bind(routes)

    println(s"Server now online. Please navigate to http://localhost:$port/test-success")
    println(s"or please navigate to http://localhost:$port/test-fail")
  }

  private def routes: Route =
    pathPrefix("test-success") {
      concat {
        pathEnd {
          concat {
            get {
              onSuccess(Future.successful {
                logger.info("hello")
                "result"
              }) { (a: String) =>
                complete(StatusCodes.OK)
              }
            }
          }
        }
      }
    } ~
      pathPrefix("test-zio-call") {
        concat {
          pathEnd {
            concat {
              get {
                onSuccess {
                  logger.info("calling zio from akka http")
                  Http().singleRequest(HttpRequest(uri = s"http://localhost:$zioPort/test"))
                } { httpResponse =>
                  logger.info(s"received ${httpResponse.status} from zio")
                  complete(httpResponse.status, httpResponse.entity.toString)
                }
              }
            }
          }
        }
      } ~
      pathPrefix("test-fail") {
        concat {
          pathEnd {
            concat {
              get {
                onComplete(Future.failed[String] {
                  throw new RuntimeException("I had a problem")
                }) { _ =>
                  complete(StatusCodes.OK)
                }
              }
            }
          }
        }
      }

}
