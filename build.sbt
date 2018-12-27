name := "la-metro-realtime"

version := "1.0"

scalaVersion := "2.12.8"

scalacOptions ++= Seq("-feature")

lazy val akkaVersion = "2.5.19"
lazy val akkaHttpVersion = "10.1.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.play" %% "play-json" % "2.6.13",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "com.squareup.okhttp3" % "mockwebserver" % "3.12.1" % Test
)
