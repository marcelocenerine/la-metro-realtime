package lametro.realtime.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, ResponseEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.typesafe.config.Config
import lametro.realtime._
import lametro.realtime.client.Reads._
import lametro.realtime.json.PlayJsonOps._

import scala.concurrent.{Future, Promise}

trait MetroApi {
  def agencies(): Future[List[Agency]]
  def routes(agencyId: AgencyId): Future[List[Route]]
  def runs(agencyId: AgencyId, routeId: RouteId): Future[List[Run]]
  def vehicles(agencyId: AgencyId): Future[List[Vehicle]]
  def stops(agencyId: AgencyId, routeId: RouteId): Future[List[Stop]]
  def stops(agencyId: AgencyId, routeId: RouteId, runId: RunId): Future[List[Stop]]
  def predictions(agencyId: AgencyId, stopId: StopId): Future[List[Prediction]]
}

private class MetroApiImpl(implicit system: ActorSystem, materializer: ActorMaterializer, config: Config)
  extends MetroApi {

  private implicit val ec = system.dispatcher
  private val baseUrl = config.getString("la-metro.metro-api.base-url")

  override def agencies(): Future[List[Agency]] =
    dispatchRequest("/agencies/").flatMap { entity =>
      Unmarshal(entity).to[List[Agency]]
    }

  override def routes(agencyId: AgencyId): Future[List[Route]] =
    dispatchRequest(s"/agencies/$agencyId/routes/").flatMap { entity =>
      Unmarshal(entity).to[Coll[Route]].map(_.items)
    }

  override def runs(agencyId: AgencyId, routeId: RouteId): Future[List[Run]] =
    dispatchRequest(s"/agencies/$agencyId/routes/$routeId/runs/")
      .flatMap { entity =>
        Unmarshal(entity).to[Coll[Run]].map(_.items)
      }

  override def vehicles(agencyId: AgencyId): Future[List[Vehicle]] =
    dispatchRequest(s"/agencies/$agencyId/vehicles/").flatMap { entity =>
      Unmarshal(entity).to[Coll[Vehicle]].map(_.items)
    }

  override def stops(agencyId: AgencyId, routeId: RouteId): Future[List[Stop]] =
    dispatchRequest(s"/agencies/$agencyId/routes/$routeId/sequence/")
      .flatMap { entity =>
        Unmarshal(entity).to[Coll[Stop]].map(_.items)
      }

  override def stops(agencyId: AgencyId, routeId: RouteId, runId: RunId): Future[List[Stop]] =
    dispatchRequest(s"/agencies/$agencyId/routes/$routeId/runs/$runId/stops/")
      .flatMap { entity =>
        Unmarshal(entity).to[Coll[Stop]].map(_.items)
      }

  override def predictions(agencyId: AgencyId, stopId: StopId): Future[List[Prediction]] =
    dispatchRequest(s"/agencies/$agencyId/stops/$stopId/predictions/")
      .flatMap { entity =>
        Unmarshal(entity).to[Coll[Prediction]].map(_.items)
      }

  private def dispatchRequest(uri: String): Future[ResponseEntity] = {
    val promise = Promise[ResponseEntity]()
    Http().singleRequest(HttpRequest(uri = baseUrl + uri)).foreach {
      case HttpResponse(OK, _, entity, _) =>
        promise.success(entity)

      case HttpResponse(status, _, entity, _) =>
        entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
          promise.failure(new RuntimeException(s"status='$status'; body='${body.utf8String}'"))
        }
    }
    promise.future
  }
}

object MetroApi {
  def apply()(implicit system: ActorSystem, materializer: ActorMaterializer, config: Config): MetroApi = new MetroApiImpl()
}