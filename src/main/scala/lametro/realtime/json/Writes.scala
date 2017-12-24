package lametro.realtime.json

import lametro.realtime._
import play.api.libs.json.Json

object Writes {
  implicit val agencyWrites = Json.writes[Agency]
  implicit val routeWrites = Json.writes[Route]
  implicit val directionWrites = Json.writes[Direction]
  implicit val runWrites = Json.writes[Run]
  implicit val geocodeWrites = Json.writes[Geocode]
  implicit val stopWrites = Json.writes[Stop]
  implicit val servicingWrites = Json.writes[Servicing]
  implicit val vehicleStatusWrites = Json.writes[VehicleStatus]
  implicit val vehicleWrites = Json.writes[Vehicle]
  implicit val predictionWrites = Json.writes[Prediction]
}
