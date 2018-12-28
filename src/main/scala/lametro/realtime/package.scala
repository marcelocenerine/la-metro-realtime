package lametro

import java.time.LocalDateTime

package object realtime {
  final case class Agency(id: AgencyId, name: String)
  final case class Route(id: RouteId, name: String)
  final case class Run(id: RunId, routeId: RouteId, name: String, direction: String)
  final case class Stop(id: Option[StopId], name: String, position: Geocode)
  final case class Vehicle(id: VehicleId, servicing: Servicing, status: VehicleStatus)
  final case class Servicing(routeId: RouteId, runId: Option[RunId])
  final case class VehicleStatus(position: Geocode, heading: Direction, predictable: Boolean, lastReportInSecs: Int) {
    val lastReport = LocalDateTime.now().minusSeconds(lastReportInSecs)
  }
  final case class Geocode(lat: Double, lon: Double)
  final case class Direction(degrees: Int)
  final case class Prediction(routeId: String, runId: String, seconds: Long)

  // TODO replace aliases with tags
  type AgencyId = String
  type RouteId = String
  type RunId = String
  type StopId = String
  type VehicleId = String
}