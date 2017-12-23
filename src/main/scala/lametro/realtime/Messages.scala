package lametro.realtime

object Messages {
  case object GetAgencies
  case class RespondAgencies(agencies: List[Agency])
  case object UnknownAgency
  case class GetVehicles(agencyId: String)
  case class GetServicingVehicles(agencyId: String, routeId: String, runId: String)
  case class RespondVehicles(vehicles: List[Vehicle])
  case object NotInSync
}
