package lametro.realtime

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.pattern.{Backoff, BackoffSupervisor, ask}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import lametro.realtime.Messages._
import lametro.realtime.json.PlayJsonOps._
import lametro.realtime.json.Writes._

import scala.concurrent._
import scala.concurrent.duration.{Duration, _}
import scala.io.StdIn
import scala.language.postfixOps

object WebServer {

  val Interface = "localhost"
  val Port = 8080

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("la-metro-system")
    implicit val ec = system.dispatcher
    implicit val materializer = ActorMaterializer()
    implicit val askTimeout: Timeout = 3 seconds

    val metroService = system.actorOf(
      BackoffSupervisor.props(
        Backoff.onStop(
          MetroServiceActor.props,
          childName = "metro-service",
          minBackoff = 3 seconds,
          maxBackoff = 30 seconds,
          randomFactor = 0.2
        )),
      name = "metro-service")

    val route =
      path("agencies") {
        get {
          complete {
            (metroService ? GetAgencies).map {
              case RespondAgencies(agencies) => agencies
            }
          }
        }
      } ~
        path("agencies" ~ Slash ~ Segment ~ Slash ~ "vehicles") { id =>
          get {
            complete {
              (metroService ? GetVehicles(id)).map {
                case RespondVehicles(vehicles) => vehicles
//                case NotInSync => "Try again"
              }
            }
          }
        }


    val bindingFuture = Http().bindAndHandle(route, Interface, Port)

    println(s"Server online at http://$Interface:$Port")

    Await.ready(bindingFuture.flatMap(_ => waitForShutdownSignal), Duration.Inf)
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }

  private def waitForShutdownSignal(implicit system: ActorSystem, ec: ExecutionContext): Future[Done] = {
    val promise = Promise[Done]()
    sys.addShutdownHook {
      promise.trySuccess(Done)
    }
    Future {
      blocking {
        if (StdIn.readLine("Press RETURN to stop...\n") != null)
          promise.trySuccess(Done)
      }
    }
    promise.future
  }
}