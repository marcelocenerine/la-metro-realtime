package lametro.realtime

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.ActorMaterializer
import lametro.realtime.Messages._
import lametro.realtime.client.MetroApi

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class MetroServiceActor extends Actor with ActorLogging {

  private implicit val system = context.system
  private implicit val ec = context.dispatcher
  private val metroApi = MetroApi(system, ActorMaterializer())
  private var agencies: List[Agency] = _
  private var agencyActors: Map[String, ActorRef] = _

  override def preStart(): Unit = {
    agencies = Await.result(metroApi.agencies(), 5 seconds) // ok to block here
    agencyActors = agencies.map { agency =>
      agency.id -> context.actorOf(AgencyActor.props(agency), s"agency-${agency.id}")
    }.toMap

    log.info("MetroService actor started")
  }

  override def postStop(): Unit = log.info("MetroService actor stopped")

  override def receive: Receive = {
    case GetAgencies =>
      sender() ! RespondAgencies(agencies)

    case msg @ GetVehicles(agencyId) =>
      forwardOrReject(agencyId, msg)

    case unknown =>
      log.warning("Unhandled message: {}", unknown)
  }

  private def forwardOrReject(agencyId: String, msg: Any): Unit =
    agencyActors.get(agencyId) match {
      case Some(agencyActor) => agencyActor forward msg
      case None => sender() ! UnknownAgency
    }
}

object MetroServiceActor {
  def props: Props = Props[MetroServiceActor]
}
