package lametro.realtime

import akka.actor.{Actor, ActorLogging, Cancellable, Props}
import com.typesafe.config.Config
import lametro.realtime.FleetActor._
import lametro.realtime.Messages.{GetServicingVehicles, GetVehicles, NotInSync, RespondVehicles}
import lametro.realtime.client.MetroApi
import lametro.realtime.config._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

private class FleetActor(agency: Agency)(implicit metroApi: MetroApi, config: Config)
  extends Actor with ActorLogging {
  import context.dispatcher

  private val syncInterval = config.getFiniteDuration("la-metro.fleet.sync-interval")
  private val cacheMaxAge = config.getFiniteDuration("la-metro.fleet.cache-max-age")
  private var syncTask: Cancellable = _

  override def preStart(): Unit = {
    syncTask = context.system.scheduler.schedule(0 second, syncInterval)(syncTimeout())
    log.info("Fleet actor {} started", agency.id)
  }

  private def syncTimeout(): Unit =
    metroApi.vehicles(agency.id)
      .onComplete {
        case Success(vehicles) =>
          self ! SyncVehicles(vehicles)
        case Failure(t) =>
          log.error(t, "Error fetching vehicles from the Metro api")
          self ! SyncFailure(t)
      }

  override def postStop(): Unit = {
    syncTask.cancel()
    log.info("Fleet actor {} stopped", agency.id)
  }

  override def receive: Receive = receiveWhenNotSynced

  private def receiveWhenNotSynced: Receive = {
    case SyncVehicles(vehicles) =>
      syncVehicles(vehicles, Map.empty)

    case SyncFailure(_) => // ignore

    case _: GetVehicles | _: GetServicingVehicles =>
      sender() ! NotInSync
  }

  private def receiveWhenSynced(vehiclesById: Map[String, Vehicle], cacheExpiryDeadline: Deadline): Receive = {
    case SyncVehicles(vehicles) =>
      syncVehicles(vehicles, vehiclesById)

    case SyncFailure(_) =>
      if (cacheExpiryDeadline.isOverdue()) {
        log.warning("Local cache is not in sync")
        context.become(receiveWhenNotSynced)
      }

    case GetVehicles(_) =>
      sender() ! RespondVehicles(vehiclesById.values.toList)

    case GetServicingVehicles(_, routeId, runId) =>
      val vehicles =
        vehiclesById.values.filter { vehicle =>
          vehicle.servicing.routeId == routeId && vehicle.servicing.runId.exists(_ == runId)
        }
      sender() ! RespondVehicles(vehicles.toList)
  }

  private def syncVehicles(newVehicles: Iterable[Vehicle], currentVehicles: Map[String, Vehicle]) = {
    val (added, changed) =
      newVehicles.foldRight((0, 0)) { case (fresh, count@(added, changed)) =>
        currentVehicles.get(fresh.id) match {
          case Some(old) if fresh != old => (added, changed + 1)
          case None => (added + 1, changed)
          case _ => count
        }
      }
    val removed = currentVehicles.size - (newVehicles.size - added)
    val newVehiclesById = newVehicles.map(vehicle => vehicle.id -> vehicle).toMap
    context.become(receiveWhenSynced(newVehiclesById, cacheMaxAge.fromNow))
    log.info("Vehicles synced: added={}, removed={}, updated={}", added, removed, changed)
  }
}

object FleetActor {
  def props(agency: Agency)(implicit metroApi: MetroApi, config: Config) = Props(new FleetActor(agency))

  private case class SyncVehicles(vehicles: Iterable[Vehicle])
  private case class SyncFailure(t: Throwable)
}