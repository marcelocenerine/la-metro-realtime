name := "la-metro-realtime"

version := "1.0"

scalaVersion := "2.12.1"

scalacOptions ++= Seq("-feature")

lazy val akkaVersion = "2.4.19"
lazy val akkaHttpVersion = "10.0.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.0.4" % Test
)