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
  def routes(agency: Agency): Future[List[Route]]
  def runs(agency: Agency, route: Route): Future[List[Run]]
  def vehicles(agency: Agency): Future[List[Vehicle]]
  def stops(agency: Agency, route: Route): Future[List[Stop]]
  def stops(agency: Agency, run: Run): Future[List[Stop]]
  def predictions(agency: Agency, stop: Stop): Future[List[Stop]]
}

private class MetroApiImpl(implicit system: ActorSystem, materializer: ActorMaterializer, config: Config)
  extends MetroApi {

  private implicit val ec = system.dispatcher
  private val baseUrl = config.getString("la-metro.metro-api.base-url")

  override def agencies(): Future[List[Agency]] =
    dispatchRequest("/agencies/").flatMap { entity =>
      Unmarshal(entity).to[List[Agency]]
    }

  override def routes(agency: Agency): Future[List[Route]] =
    dispatchRequest(s"/agencies/${agency.id}/routes/").flatMap { entity =>
      Unmarshal(entity).to[Coll[Route]].map(_.items)
    }

  override def runs(agency: Agency, route: Route): Future[List[Run]] =
    dispatchRequest(s"/agencies/${agency.id}/routes/${route.id}/runs/")
      .flatMap { entity =>
        Unmarshal(entity).to[Coll[Run]]
          .map(_.items.map(_.copy(id = route.id))) // bug in the api
      }

  override def vehicles(agency: Agency): Future[List[Vehicle]] =
    dispatchRequest(s"/agencies/${agency.id}/vehicles/").flatMap { entity =>
      Unmarshal(entity).to[Coll[Vehicle]].map(_.items)
    }

  override def stops(agency: Agency, route: Route): Future[List[Stop]] =
    dispatchRequest(s"/agencies/${agency.id}/routes/${route.id}/sequence/")
      .flatMap { entity =>
        Unmarshal(entity).to[Coll[Stop]].map(_.items)
      }

  override def stops(agency: Agency, run: Run): Future[List[Stop]] =
    dispatchRequest(s"/agencies/${agency.id}/routes/${run.routeId}/runs/${run.id}/stops/")
      .flatMap { entity =>
        Unmarshal(entity).to[Coll[Stop]].map(_.items)
      }

  override def predictions(agency: Agency, stop: Stop): Future[List[Stop]] =
    dispatchRequest(s"/agencies/${agency.id}/stops/${stop.id}/predictions/")
      .flatMap { entity =>
        Unmarshal(entity).to[Coll[Stop]].map(_.items)
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