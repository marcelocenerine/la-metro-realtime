package lametro.realtime.json

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.server.{RejectionError, ValidationRejection}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString
import lametro.realtime.json.PlayJsonOps.PlayJsonError
import play.api.libs.json._

trait PlayJsonOps {
  private val jsonStringUnmarshaller =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(`application/json`)
      .mapWithCharset {
        case (ByteString.empty, _) => throw Unmarshaller.NoContentException
        case (data, charset) => data.decodeString(charset.nioCharset.name)
      }

  implicit def unmarshaller[A: Reads]: FromEntityUnmarshaller[A] = {
    def read(json: JsValue) =
      implicitly[Reads[A]]
        .reads(json)
        .recoverTotal { e =>
          throw RejectionError(ValidationRejection(JsError.toJson(e).toString, Some(PlayJsonError(e))))
        }

    jsonStringUnmarshaller.map(data => read(Json.parse(data)))
  }

  private val jsonStringMarshaller =
    Marshaller.stringMarshaller(`application/json`)

  implicit def marshaller[A : Writes]: ToEntityMarshaller[A] =
    jsonStringMarshaller
      .compose(Json.stringify)
      .compose(implicitly[Writes[A]].writes)
}

object PlayJsonOps extends PlayJsonOps {
  case class PlayJsonError(error: JsError) extends RuntimeException {
    override def getMessage: String = JsError.toJson(error).toString()
  }
}