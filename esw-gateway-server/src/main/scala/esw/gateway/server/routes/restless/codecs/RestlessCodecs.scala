package esw.gateway.server.routes.restless.codecs

import com.github.ghik.silencer.silent
import csw.alarm.codecs.AlarmCodecs
import csw.location.client.HttpCodecs
import csw.location.models.codecs.LocationCodecs
import csw.params.core.formats.{CodecHelpers, ParamCodecs}
import esw.gateway.server.routes.restless.messages.ErrorResponseMsg._
import esw.gateway.server.routes.restless.messages.RequestMsg._
import esw.gateway.server.routes.restless.messages._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait RestlessCodecs extends ParamCodecs with LocationCodecs with HttpCodecs with AlarmCodecs with EitherCodecs {

  implicit def responseMsgCodec[T <: ErrorResponseMsg]: Codec[T] = responseMsgCodecValue.asInstanceOf[Codec[T]]

  lazy val responseMsgCodecValue: Codec[ErrorResponseMsg] = {
    @silent implicit lazy val noEventKeysCodec: Codec[NoEventKeys]                         = deriveCodec[NoEventKeys]
    @silent implicit lazy val invalidComponentCodec: Codec[InvalidComponent]               = deriveCodec[InvalidComponent]
    @silent implicit lazy val setAlarmSeverityFailureCodec: Codec[SetAlarmSeverityFailure] = deriveCodec[SetAlarmSeverityFailure]
    deriveCodec[ErrorResponseMsg]
  }

  implicit def routeMsgCodec[T <: RequestMsg]: Codec[T] = routeMsgCodecValue.asInstanceOf[Codec[T]]

  lazy val routeMsgCodecValue: Codec[RequestMsg] = {
    @silent implicit lazy val commandMsgCodec: Codec[CommandMsg]                   = deriveCodec[CommandMsg]
    @silent implicit lazy val publishEventMsgCodec: Codec[PublishEventMsg]         = deriveCodec[PublishEventMsg]
    @silent implicit lazy val getEventMsgCodec: Codec[GetEventMsg]                 = deriveCodec[GetEventMsg]
    @silent implicit lazy val setAlarmSeverityMsgCodec: Codec[SetAlarmSeverityMsg] = deriveCodec[SetAlarmSeverityMsg]
    deriveCodec[RequestMsg]
  }

  implicit lazy val commandActionCode: Codec[CommandAction] = CodecHelpers.enumCodec[CommandAction]

}
