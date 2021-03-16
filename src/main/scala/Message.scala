import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class Message(title: String, text: String)

trait MessageJsonProtocol extends DefaultJsonProtocol {
  implicit val messageFormat: RootJsonFormat[Message] = jsonFormat2(Message)
}
