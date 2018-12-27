package lametro.realtime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.typesafe.config.Config
import lametro.realtime.Messages._
import lametro.realtime.client.MetroApi

private class AgencyActor(agency: Agency)(implicit metroApi: MetroApi, config: Config)
  extends Actor with ActorLogging {

  private val fleetActor: ActorRef = context.actorOf(FleetActor.props(agency), "fleet")

  override def preStart(): Unit = log.info("Agency actor {} started", agency.id)

  override def postStop(): Unit = log.info("Agency actor {} stopped", agency.id)

  override def receive: Receive = {
    case msg @ GetVehicles(agency.id) => fleetActor forward msg
    case msg @ GetServicingVehicles(agency.id, _, _) => fleetActor forward msg
  }
}

object AgencyActor {
  def props(agency: Agency)(implicit metroApi: MetroApi, config: Config) = Props(new AgencyActor(agency))
}
