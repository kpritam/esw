package esw.gateway.api.clients

import akka.Done
import csw.alarm.models.AlarmSeverity
import csw.location.api.codec.DoneCodec
import csw.params.core.models.Subsystem
import esw.gateway.api.AlarmServiceApi
import esw.gateway.api.codecs.RestlessCodecs
import esw.gateway.api.messages.GatewayHttpRequest.SetAlarmSeverity
import esw.gateway.api.messages.SetAlarmSeverityFailure
import msocket.api.{EitherCodecs, HttpClient}

import scala.concurrent.Future

class AlarmClient(httpClient: HttpClient) extends AlarmServiceApi with RestlessCodecs with EitherCodecs with DoneCodec {

  override def setSeverity(
      subsystem: Subsystem,
      componentName: String,
      alarmName: String,
      severity: AlarmSeverity
  ): Future[Either[SetAlarmSeverityFailure, Done]] = {
    httpClient.post[SetAlarmSeverity, Either[SetAlarmSeverityFailure, Done]](
      SetAlarmSeverity(subsystem, componentName, alarmName, severity)
    )
  }
}
