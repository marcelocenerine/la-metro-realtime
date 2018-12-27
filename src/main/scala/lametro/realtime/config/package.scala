package lametro.realtime

import com.typesafe.config.Config

import scala.concurrent.duration._

package object config {
  implicit class ConfigOps(val config: Config) extends AnyVal {
    def getFiniteDuration(path: String): FiniteDuration =
      config.getDuration(path).toMillis.millis
  }
}
