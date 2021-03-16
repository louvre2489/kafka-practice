name := "kafka-practice"

version := "0.1"

scalaVersion := "2.13.4"

val AkkaVersion = "2.6.9"
val AkkaStreamKafkaVersion = "2.0.7"

val dependencies = List(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % AkkaStreamKafkaVersion,

  "io.spray" %%  "spray-json" % "1.3.6",
  "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

libraryDependencies ++= dependencies
