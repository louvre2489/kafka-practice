import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Failure, Success}

import akka.{Done, NotUsed}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.{
  CommitterSettings,
  ConsumerSettings,
  ProducerMessage,
  ProducerSettings,
  Subscriptions
}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.stream.{ActorMaterializer, KillSwitches}
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{
  StringDeserializer,
  StringSerializer
}
import spray.json._

object Main extends App with MessageJsonProtocol {

  val topic = "practice-topic"
  val copyTopic = "copy-topic"
  val group = "practice-group"

  implicit val system: ActorSystem[Nothing] =
    ActorSystem(Behaviors.empty, "KafkaPractice")
  implicit val ec: ExecutionContext = system.executionContext

  val conf = ConfigFactory.load()
  val cBootstrapServers = conf.getString("kafka.consumer.bootstrapServers")
  val pBootstrapServers = conf.getString("kafka.producer.bootstrapServers")

  val consumerSettings =
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(cBootstrapServers)
      .withGroupId(group)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val producerSettings =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(pBootstrapServers)

  val committerSettings = CommitterSettings(system)

  val (cKillSwitch, cDone) = Consumer
    .committableSource(consumerSettings, Subscriptions.topics(topic))
    .viaMat(KillSwitches.single)(Keep.right)
    .map { consumed =>
      println("Consuming!!!")
      try {
        val msg = consumed.record.value().parseJson.convertTo[Message]
        println(s"success. title: ${msg.title}, text: ${msg.text}")

        val done =
          Source
          .single(msg)
          .map {_ =>
              new ProducerRecord[String, String](copyTopic, s"copy message - title:${msg.title}, text:${msg.text}")
          }
          .runWith(Producer.plainSink(producerSettings))

        done.onComplete {
          case Success(_) =>
            println("Finish Send Message!")
          case Failure(ex) =>
            println(s"Fail Send. Reason: ${ex.getMessage}")
        }
      } catch {
        case _: Throwable =>
          println("invalid text.")
      }
      consumed
    }
    .mapAsync(1) { msg =>
      msg.committableOffset.commitScaladsl()
    }
    .toMat(Sink.ignore)(Keep.both)
    .run()

  cDone.onComplete {
    case Success(_) =>
      println("done consumer.")
    case Failure(ex) =>
      println(s"fail consume. reason: ${ex.getMessage}")
  }

  sys.addShutdownHook {
    cKillSwitch.shutdown()
  }

  println("start")
}
