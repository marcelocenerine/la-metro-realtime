package lametro.realtime.client

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import lametro.realtime._
import okhttp3.mockwebserver.{Dispatcher, MockResponse, MockWebServer, RecordedRequest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.duration._

class MetroApiSpec extends TestKit(ActorSystem("test"))
  with FlatSpecLike
  with ScalaFutures
  with Matchers {

  private val agencyId = "lametro"
  private implicit val patience = PatienceConfig(timeout = 5.seconds)

  behavior of "api client"

  it should "return agencies" in {
    val jsonResponse =
      """
        |[
        |  {
        |    "display_name": "Los Angeles Metro",
        |    "id": "lametro"
        |  },
        |  {
        |    "display_name": "Los Angeles Metro Rail",
        |    "id": "lametro-rail"
        |  }
        |]
      """.stripMargin

    withMetroApi("GET /agencies/", jsonResponse) { client =>
      whenReady(client.agencies()) { agencies =>
        agencies shouldBe List(
          Agency(id = "lametro", name = "Los Angeles Metro"),
          Agency(id = "lametro-rail", name = "Los Angeles Metro Rail")
        )
      }
    }
  }

  it should "return routes for a given agency" in {
    val jsonResponse =
      """
        |{
        |  "items": [
        |    {
        |      "id": "2",
        |      "display_name": "2 Downtown LA - Westwood"
        |    },
        |    {
        |      "id": "4",
        |      "display_name": "4 Downtown LA - Santa Monica Via Santa"
        |    },
        |    {
        |      "id": "10",
        |      "display_name": "10 W Hollywood-Dtwn LA -Avalon Sta Via"
        |    },
        |    {
        |      "id": "14",
        |      "display_name": "14 Beverly Hlls-Dtwn LA-Wash/Fairfax Vi"
        |    }
        |  ]
        |}
      """.stripMargin

    withMetroApi(s"GET /agencies/$agencyId/routes/", jsonResponse) { client =>
      whenReady(client.routes(agencyId)) { routes =>
        routes shouldBe List(
          Route(id = "2", name = "2 Downtown LA - Westwood"),
          Route(id = "4", name = "4 Downtown LA - Santa Monica Via Santa"),
          Route(id = "10", name = "10 W Hollywood-Dtwn LA -Avalon Sta Via"),
          Route(id = "14", name = "14 Beverly Hlls-Dtwn LA-Wash/Fairfax Vi")
        )
      }
    }
  }

  it should "return runs for a given route" in {
    val routeId = "4"
    val jsonResponse =
      """
        |{
        |  "items": [
        |    {
        |      "route_id": "4_276_0",
        |      "direction_name": "East",
        |      "id": "4_276_0",
        |      "display_in_ui": true,
        |      "display_name": "East to Downtown LA - Broadway-Venice"
        |    },
        |    {
        |      "route_id": "4_260_1",
        |      "direction_name": "West",
        |      "id": "4_260_1",
        |      "display_in_ui": true,
        |      "display_name": "West to West LA - Sepulveda Bl"
        |    }
        |  ]
        |}
      """.stripMargin

    withMetroApi(s"GET /agencies/$agencyId/routes/$routeId/runs/", jsonResponse) { client =>
      whenReady(client.runs(agencyId, routeId)) { runs =>
        runs shouldBe List(
          Run(id = "4_276_0", routeId = "4", name = "East to Downtown LA - Broadway-Venice", direction = "East"),
          Run(id = "4_260_1", routeId = "4", name = "West to West LA - Sepulveda Bl", direction = "West")
        )
      }
    }
  }

  it should "return vehicles for a given agency" in {
    val jsonResponse =
      """
        |{
        |  "items": [
        |    {
        |      "latitude": 34.204033,
        |      "seconds_since_report": 64,
        |      "heading": 345,
        |      "route_id": "794",
        |      "longitude": -118.348694,
        |      "run_id": "794_54_0",
        |      "id": "8084",
        |      "predictable": true
        |    },
        |    {
        |      "latitude": 34.0718045,
        |      "seconds_since_report": 10,
        |      "heading": 84,
        |      "route_id": "45",
        |      "longitude": -118.2262125,
        |      "run_id": "45_518_0",
        |      "id": "8179",
        |      "predictable": false
        |    },
        |    {
        |      "latitude": 34.157585,
        |      "seconds_since_report": 187,
        |      "heading": 179,
        |      "route_id": "744",
        |      "longitude": -118.44883,
        |      "id": "9277",
        |      "predictable": true
        |    }
        |  ]
        |}
      """.stripMargin

    withMetroApi(s"GET /agencies/$agencyId/vehicles/", jsonResponse) { client =>
      whenReady(client.vehicles(agencyId)) { vehicles =>
        vehicles shouldBe List(
          Vehicle(
            id = "8084",
            servicing = Servicing(routeId = "794", runId = Some("794_54_0")),
            status =
              VehicleStatus(
                position = Geocode(lat = 34.204033, lon = -118.348694),
                heading = Direction(degrees = 345),
                predictable = true,
                lastReportInSecs = 64
              )
          ),
          Vehicle(
            id = "8179",
            servicing = Servicing(routeId = "45", runId = Some("45_518_0")),
            status =
              VehicleStatus(
                position = Geocode(lat = 34.0718045, lon = -118.2262125),
                heading = Direction(degrees = 84),
                predictable = false,
                lastReportInSecs = 10
              )
          ),
          Vehicle(
            id = "9277",
            servicing = Servicing(routeId = "744", runId = None),
            status =
              VehicleStatus(
                position = Geocode(lat = 34.157585, lon = -118.44883),
                heading = Direction(degrees = 179),
                predictable = true,
                lastReportInSecs = 187
              )
          )
        )
      }
    }
  }

  it should "return stops for a given route" in {
    val routeId = "704"
    val jsonResponse =
      """
        |{
        |  "items": [
        |    {
        |      "latitude": 34.0158299,
        |      "display_name": "Ocean / Arizona",
        |      "longitude": -118.49987,
        |      "id": "04098"
        |    },
        |    {
        |      "latitude": 34.0166499,
        |      "display_name": "Santa Monica / 4th",
        |      "longitude": -118.49468,
        |      "id": "14360"
        |    },
        |    {
        |      "longitude": -118.23082,
        |      "latitude": 34.0566999,
        |      "display_name": "Division 13 Layover"
        |    }
        |  ]
        |}
      """.stripMargin

    withMetroApi(s"GET /agencies/$agencyId/routes/$routeId/sequence/", jsonResponse) { client =>
      whenReady(client.stops(agencyId, routeId)) { stops =>
        stops shouldBe List(
          Stop(id = Some("04098"), name = "Ocean / Arizona", position = Geocode(lat = 34.0158299, lon = -118.49987)),
          Stop(id = Some("14360"), name = "Santa Monica / 4th", position = Geocode(lat = 34.0166499, lon = -118.49468)),
          Stop(id = None, name = "Division 13 Layover", position = Geocode(lat = 34.0566999, lon = -118.23082))
        )
      }
    }
  }

  it should "return stops for a given run" in {
    val routeId = "704"
    val runId = "704_168_1"
    val jsonResponse =
      """
        |{
        |  "items": [
        |    {
        |      "latitude": 34.0566999,
        |      "id": "03708",
        |      "display_name": "Division 13 Layover",
        |      "longitude": -118.23082
        |    },
        |    {
        |      "latitude": 34.0553899,
        |      "id": "30000",
        |      "display_name": "Patsaouras Transit Plaza",
        |      "longitude": -118.23312
        |    },
        |    {
        |      "latitude": 34.0588099,
        |      "id": "09221",
        |      "display_name": "Cesar E Chavez / Broadway",
        |      "longitude": -118.2403999
        |    }
        |  ]
        |}
      """.stripMargin

    withMetroApi(s"GET /agencies/$agencyId/routes/$routeId/runs/$runId/stops/", jsonResponse) { client =>
      whenReady(client.stops(agencyId, routeId, runId)) { stops =>
        stops shouldBe List(
          Stop(id = Some("03708"), name = "Division 13 Layover", position = Geocode(lat = 34.0566999, lon = -118.23082)),
          Stop(id = Some("30000"), name = "Patsaouras Transit Plaza", position = Geocode(lat = 34.0553899, lon = -118.23312)),
          Stop(id = Some("09221"), name = "Cesar E Chavez / Broadway", position = Geocode(lat = 34.0588099, lon = -118.2403999))
        )
      }
    }
  }

  it should "return predictions for a given stop" in {
    val stopId = "01297"
    val jsonResponse =
      """
        |{
        |  "items": [
        |    {
        |      "route_id": "704",
        |      "is_departing": false,
        |      "seconds": 186,
        |      "block_id": "7044500",
        |      "minutes": 3,
        |      "run_id": "704_149_1"
        |    },
        |    {
        |      "route_id": "16",
        |      "is_departing": false,
        |      "seconds": 686,
        |      "block_id": "0161402",
        |      "minutes": 11,
        |      "run_id": "16_367_1"
        |    },
        |    {
        |      "route_id": "316",
        |      "is_departing": false,
        |      "seconds": 3294,
        |      "block_id": "0166602",
        |      "minutes": 54,
        |      "run_id": "316_374_1"
        |    }
        |  ]
        |}
      """.stripMargin

    withMetroApi(s"GET /agencies/$agencyId/stops/$stopId/predictions/", jsonResponse) { client =>
      whenReady(client.predictions(agencyId, stopId)) { predictions =>
        predictions shouldBe List(
          Prediction(routeId = "704", runId = "704_149_1", seconds = 186),
          Prediction(routeId = "16", runId = "16_367_1", seconds = 686),
          Prediction(routeId = "316", runId = "316_374_1", seconds = 3294)
        )
      }
    }
  }

  private def withMetroApi(expectedReq: String, response: String)(fn: MetroApi => Any): Unit = {
    val ws = new MockWebServer()
    ws.setDispatcher(new Dispatcher {
      override def dispatch(request: RecordedRequest): MockResponse = {
        if (request.getRequestLine == s"$expectedReq HTTP/1.1") {
          new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(response)
        } else {
          new MockResponse().setResponseCode(404)
        }
      }
    })
    ws.start()
    val baseUrl = s"http://localhost:${ws.getPort}"
    implicit val materializer = ActorMaterializer()
    implicit val config = ConfigFactory.parseString(s"""la-metro.metro-api.base-url = "$baseUrl"""")

    try {
      fn(MetroApi())
    } finally {
      ws.shutdown()
    }
  }
}
