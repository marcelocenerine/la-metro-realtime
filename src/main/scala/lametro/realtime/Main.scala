package lametro.realtime

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import lametro.realtime.Messages.{RespondAgencies, _}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

object Main extends App {

  implicit val system = ActorSystem("la-metro")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(10 seconds)

  val metroService = system.actorOf(MetroServiceActor.props, "metro-service")

  val RespondAgencies(agencies) = Await.result(metroService ? GetAgencies, 5 seconds)

  for {
    agency <- agencies
  } {
    Thread.sleep(5000) // wait for syncing
    val response = Await.result(metroService ? GetVehicles(agency.id), 2 seconds)
    println(s"Agency: $agency")
    response match {
      case RespondVehicles(vehicles) =>
        println("Vehicles:")
        vehicles.foreach(v => println(s"\t- $v"))

      case NotInSync =>
        println("Vehicles not in sync")
    }
  }

  println(">>> Press ENTER to exit <<<")
  StdIn.readLine()
  system.terminate()
}
