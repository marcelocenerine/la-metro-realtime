package lametro.realtime

import java.time.LocalDateTime

final case class Agency(id: String, name: String)
final case class Route(id: String, name: String)
final case class Run(id: String, routeId: String, name: String, direction: String)
final case class Stop(id: Option[String], name: String, position: Geocode)
final case class Vehicle(id: String, servicing: Servicing, status: VehicleStatus)
final case class Servicing(routeId: String, runId: Option[String])
final case class VehicleStatus(position: Geocode, heading: Direction, predictable: Boolean, lastReportInSecs: Int) {
  val lastReport = LocalDateTime.now().minusSeconds(lastReportInSecs)
}
final case class Geocode(lat: Double, lon: Double)
final case class Direction(degrees: Int)
final case class Prediction(routeId: String, runId: String, minutes: Int)