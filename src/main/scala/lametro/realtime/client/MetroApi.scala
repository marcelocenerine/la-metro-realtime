package lametro.realtime.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import lametro.realtime.client.Json._
import lametro.realtime.client.MetroApi._
import lametro.realtime.json.PlayJsonOps._
import lametro.realtime.{Agency, Route, Stop, Vehicle}

import scala.concurrent.Future

class MetroApi(implicit system: ActorSystem, materializer: ActorMaterializer) {

  private implicit val ec = system.dispatcher

  def agencies(): Future[List[Agency]] =
    dispatchRequest(s"$BaseUrl/agencies/", entity => Unmarshal(entity).to[List[Agency]])

  def routes(agency: Agency): Future[List[Route]] =
    dispatchRequest(s"$BaseUrl/agencies/${agency.id}/routes/",
      entity => Unmarshal(entity).to[Coll[Route]].map(_.items))

  def vehicles(agency: Agency): Future[List[Vehicle]] =
    dispatchRequest(s"$BaseUrl/agencies/${agency.id}/vehicles/",
      entity => Unmarshal(entity).to[Coll[Vehicle]].map(_.items))

  def stops(agency: Agency, route: Route): Future[List[Stop]] =
    dispatchRequest(s"$BaseUrl/agencies/${agency.id}/routes/${route.id}/sequence/",
      entity => Unmarshal(entity).to[Coll[Stop]].map(_.items))

  private def dispatchRequest[T](uri: String, unmarshaller: ResponseEntity => Future[T]): Future[T] =
    Http().singleRequest(HttpRequest(uri = uri)).flatMap {
      case HttpResponse(OK, _, entity, _) =>
        unmarshaller(entity)

      case resp@HttpResponse(status, _, _, _) =>
        resp.discardEntityBytes()
        throw new RuntimeException(s"error: $status")
    }
}

object MetroApi {
  private val BaseUrl = "http://api.metro.net"
}

// Agencies: http://api.metro.net/agencies
// Routes: http://api.metro.net/agencies/lametro/routes/
// Route: http://api.metro.net/agencies/lametro/routes/14/info/ (colors)
// Runs: http://api.metro.net/agencies/lametro/routes/20/runs/
// Stops: http://api.metro.net/agencies/lametro/routes/20/stops/
// Vehicles: http://api.metro.net/agencies/lametro/vehicles/
// https://developer.metro.net/introduction/tp-information-feed/overview/
// http://alexkuang.com/blog/2016/04/26/writing-an-api-client-with-akka-http/