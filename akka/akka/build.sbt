name := "akka"

version := "0.1"

scalaVersion := "2.13.2"

val akkaVersion = "2.6.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
  "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "2.0.0",
  "org.postgresql" % "postgresql" % "42.2.12",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-simple" % "1.7.5"
)
