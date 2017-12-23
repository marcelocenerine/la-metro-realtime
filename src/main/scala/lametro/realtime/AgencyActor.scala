package lametro.realtime

import java.time.Clock

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import lametro.realtime.Messages._

class AgencyActor(agency: Agency) extends Actor with ActorLogging {

  private var fleetActor: ActorRef = _

  override def preStart(): Unit = {
    fleetActor = context.actorOf(FleetActor.props(agency, Clock.systemDefaultZone()), "fleet")

    log.info("Agency actor {} started", agency.id)
  }

  override def postStop(): Unit = log.info("Agency actor {} stopped", agency.id)

  override def receive: Receive = {
    case msg @ GetVehicles(agency.id) =>
      fleetActor forward msg

    case msg @ GetServicingVehicles(agency.id, _, _) =>
      fleetActor forward msg

    case unknown =>
      log.warning("Unhandled message: {}", unknown)
  }
}

object AgencyActor {
  def props(agency: Agency): Props = Props(new AgencyActor(agency))
}
