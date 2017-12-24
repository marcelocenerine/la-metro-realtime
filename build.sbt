name := "la-metro-realtime"

version := "1.0"

scalaVersion := "2.12.1"

scalacOptions ++= Seq("-feature")

lazy val akkaVersion = "2.5.8"
lazy val akkaHttpVersion = "10.0.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.0.4" % Test
)

dependencyOverrides += "com.typesafe.akka" %% "akka-actor" % akkaVersion
dependencyOverrides += "com.typesafe.akka" %% "akka-stream" % akkaVersion