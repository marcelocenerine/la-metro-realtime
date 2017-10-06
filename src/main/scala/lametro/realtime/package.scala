package lametro.realtime

import java.time.LocalDateTime

final case class Agency(id: String, name: String)
final case class Route(id: String, name: String)
final case class Stop(id: Option[String], name: String, position: Geocode)
final case class Vehicle(id: String, routeId: String, status: VehicleStatus)
final case class VehicleStatus(position: Geocode, heading: Direction, predictable: Boolean, reported: LocalDateTime)
final case class Geocode(lat: Double, lon: Double)
final case class Direction(degrees: Int)