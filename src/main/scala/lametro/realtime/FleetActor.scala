package lametro.realtime

import java.time.{Clock, Instant}

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.ActorMaterializer
import lametro.realtime.FleetActor._
import lametro.realtime.Messages.{GetServicingVehicles, GetVehicles, NotInSync, RespondVehicles}
import lametro.realtime.client.MetroApi

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

private class FleetActor(agency: Agency, clock: Clock) extends Actor with ActorLogging {

  private implicit val system = context.system
  private implicit val ec = context.dispatcher
  private implicit val materializer = ActorMaterializer()
  private val metroApi = MetroApi()
  private var lastRefresh: Option[Instant] = None
  private var vehiclesById = Map.empty[String, Vehicle]

  override def preStart(): Unit = {
    system.scheduler.schedule(0 second, 10 seconds)(syncTimeout())
    log.info("Fleet actor {} started", agency.id)
  }

  private def syncTimeout() =
    metroApi.vehicles(agency).onComplete {
      case Success(vehicles) =>
        self ! SyncVehicles(vehicles)

      case Failure(t) =>
        log.error(t, "Error to fetch vehicles from Metro api")
        self ! SyncFailure
    }

  override def postStop(): Unit = {
    materializer.shutdown()
    log.info("Fleet actor {} stopped", agency.id)
  }

  override def receive: Receive = receiveWhenNotSynced

  private def receiveWhenNotSynced: Receive = {
    case SyncVehicles(vehicles) =>
      syncVehicles(vehicles)
      context.become(receiveWhenSynced)

    case SyncFailure => // ignore

    case GetVehicles(_) | GetServicingVehicles(_, _, _) =>
      sender() ! NotInSync

    case unknown =>
      log.warning("Unhandled message: {}", unknown)
  }

  private def receiveWhenSynced: Receive = {
    case SyncVehicles(vehicles) =>
      syncVehicles(vehicles)

    case SyncFailure =>
      if (needsResync) context.unbecome()

    case GetVehicles(_) =>
      sender() ! RespondVehicles(vehiclesById.values.toList)

    case GetServicingVehicles(_, routeId, runId) =>
      val vehicles = vehiclesById.values.filter { v =>
          v.servicing.routeId == routeId &&
            v.servicing.runId.fold(false)(_ == runId)
        }.toList
      sender() ! RespondVehicles(vehicles)

    case unknown =>
      log.warning("Unhandled message: {}", unknown)
  }

  private def syncVehicles(vehicles: Iterable[Vehicle]) = {
    val (added, changed) =
      vehicles.foldRight((0, 0)) { case (fresh, count@(added, changed)) =>
        vehiclesById.get(fresh.id) match {
          case Some(old) if fresh != old => (added, changed + 1)
          case None => (added + 1, changed)
          case _ => count
        }
      }
    val removed = vehiclesById.size - (vehicles.size - added)

    vehiclesById = vehicles.map(v => (v.id, v)).toMap
    lastRefresh = Some(clock.instant())
    log.info("Vehicles synced: added={}, removed={}, updated={}", added, removed, changed)
  }

  private def needsResync: Boolean = lastRefresh match {
    case Some(instant) if !instant.plusMillis(MaxIntervalWithoutSync.toMillis).isBefore(clock.instant()) => false
    case _ => true
  }
}

object FleetActor {
  val MaxIntervalWithoutSync = 5 minutes

  def props(agency: Agency, clock: Clock): Props = Props(new FleetActor(agency, clock))

  private case class SyncVehicles(vehicles: Iterable[Vehicle])
  private case object SyncFailure
}