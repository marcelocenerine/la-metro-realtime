package lametro.realtime.client

import lametro.realtime._
import play.api.libs.functional.syntax._
import play.api.libs.json._

private [client] object Reads {

  implicit val agencyReads: Reads[Agency] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "display_name").read[String]
    ) (Agency.apply _)

  implicit val routeReads: Reads[Route] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "display_name").read[String]
    ) (Route.apply _)

  implicit val runReads: Reads[Run] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "route_id").read[String] and
      (JsPath \ "display_name").read[String] and
      (JsPath \ "direction_name").read[String]
    ) (Run.apply _)

  implicit val predictionReads: Reads[Prediction] = (
    (JsPath \ "route_id").read[String] and
      (JsPath \ "run_id").read[String] and
      (JsPath \ "minutes").read[Int]
    ) (Prediction.apply _)

  implicit val vehicleReads: Reads[Vehicle] = {
    case json: JsObject =>
      val vehicle =
        Vehicle(
          id = (json \ "id").as[String],
          servicing =
            Servicing(
              routeId = (json \ "route_id").as[String],
              runId = (json \ "run_id").asOpt[String]
            ),
          status =
            VehicleStatus(
              position =
                Geocode(
                  lat = (json \ "latitude").as[Double],
                  lon = (json \ "longitude").as[Double]
                ),
              heading = Direction((json \ "heading").as[Int]),
              predictable = (json \ "predictable").as[Boolean],
              lastReportInSecs = (json \ "seconds_since_report").as[Int]
            )
        )
      JsSuccess(vehicle)
    case _ => JsError("Unexpected JSON format")
  }

  implicit val stopReads: Reads[Stop] = {
    case json: JsObject =>
      val stop =
        Stop(
          id = (json \ "id").asOpt[String],
          name = (json \ "display_name").as[String],
          position =
            Geocode(
              lat = (json \ "latitude").as[Double],
              lon = (json \ "longitude").as[Double]
            )
        )
      JsSuccess(stop)
    case _ => JsError("Unexpected JSON format")
  }

  implicit def collReads[T: Reads]: Reads[Coll[T]] = {
    case json: JsObject =>
      JsSuccess(Coll((json \ "items").as[List[T]]))
    case _ => JsError("Unexpected JSON format")
  }

  final case class Coll[T](items: List[T])
}
