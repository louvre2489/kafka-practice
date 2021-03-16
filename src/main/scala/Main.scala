import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.{
  CommitterSettings,
  ConsumerSettings,
  ProducerSettings,
  Subscriptions
}
import akka.kafka.scaladsl.{Committer, Consumer, Producer}
import akka.stream.KillSwitches
import akka.stream.scaladsl.Keep
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

  val createProducerRecord: Message => ProducerRecord[String, String] = msg =>
    new ProducerRecord[String, String](
      copyTopic,
      s"copy message - title:${msg.title}, text:${msg.text}"
  )

  val done: Future[Done] = Consumer
    .committableSource(consumerSettings, Subscriptions.topics(topic))
    .viaMat(KillSwitches.single)(Keep.right)
    .map { consumed =>
      println("Consuming!!!")

      val msg = consumed.record.value().parseJson.convertTo[Message]
      consumed.committableOffset.commitScaladsl()

      msg
    }
    .recover {
      case _: Throwable =>
        Message(title = "Error", text = "Invalid Message")
    }
    .map { msg =>
      createProducerRecord(msg)
    }
    .mapAsync(1) { producing =>
      Future.apply(producing)
    }
    .runWith(Producer.plainSink(producerSettings))

  done.onComplete {
    case Success(_) =>
      println("done.")
    case Failure(ex) =>
      println(s"fail. reason: ${ex.getMessage}")
  }

  println("start")
}
