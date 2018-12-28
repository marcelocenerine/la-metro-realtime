package lametro.realtime

object Messages {
  case object GetAgencies
  case class RespondAgencies(agencies: List[Agency])
  case object UnknownAgency
  case class GetVehicles(agencyId: AgencyId)
  case class GetServicingVehicles(agencyId: AgencyId, routeId: RouteId, runId: RunId)
  case class RespondVehicles(vehicles: List[Vehicle])
  case object NotInSync
}
