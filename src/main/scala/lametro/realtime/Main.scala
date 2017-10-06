package lametro.realtime

import java.time.Clock

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import lametro.realtime.client.MetroApi

import scala.concurrent.{Await, Future}
import scala.io.StdIn
import scala.util.{Failure, Success}
import scala.concurrent.duration._

object Main extends App {

  implicit val system = ActorSystem("la-metro")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  implicit val systemClock = Clock.systemDefaultZone()

  val metroApi = new MetroApi()

  metroApi.agencies().onComplete {
    case Success(agencies) =>
      for (agency <- agencies) {
        val routes = Await.result(metroApi.routes(agency), 5.seconds)
        val vehicles = Await.result(metroApi.vehicles(agency), 5.seconds)

        val futureStops =
          for (route <- routes)
            yield metroApi.stops(agency, route).recover {
              case e =>
                e.printStackTrace()
                List.empty
            }.map((route, _))

        val stops = Await.result(Future.sequence(futureStops), 10.seconds).toMap

        println(agency)
        for (route <- routes) {
          println(s"\t- $route")
          for {
            maybeStop <- stops.get(route)
            stop <- maybeStop
          } {
            println(s"\t\t- $stop")
          }
        }

        println()
        println("* Vehicles:")
        for (vehicle <- vehicles) {
          println(s"\t- $vehicle")
        }
      }

    case Failure(ex) => ex.printStackTrace()
  }


  println(">>> Press ENTER to exit <<<")
  StdIn.readLine()
  system.terminate()
}
