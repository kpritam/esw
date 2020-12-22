package esw.gateway.api.clients

import akka.Done
import csw.location.api.models.ComponentId
import csw.logging.models.codecs.LoggingCodecs
import csw.logging.models.{Level, LogMetadata}
import esw.gateway.api.AdminApi
import esw.gateway.api.protocol.GatewayRequest
import esw.gateway.api.protocol.GatewayRequest.{GetLogMetadata, GoOffline, GoOnline, Restart, SetLogLevel, Shutdown}
import msocket.api.Transport
import msocket.api.codecs.BasicCodecs

import scala.concurrent.Future

class AdminClient(postClient: Transport[GatewayRequest]) extends AdminApi with LoggingCodecs with BasicCodecs {

  override def shutdown(componentId: ComponentId): Future[Done] = postClient.requestResponse[Done](Shutdown(componentId))

  override def restart(componentId: ComponentId): Future[Done] = postClient.requestResponse[Done](Restart(componentId))

  override def goOffline(componentId: ComponentId): Future[Done] = postClient.requestResponse[Done](GoOffline(componentId))

  override def goOnline(componentId: ComponentId): Future[Done] = postClient.requestResponse[Done](GoOnline(componentId))

  def getLogMetadata(componentId: ComponentId): Future[LogMetadata] =
    postClient.requestResponse[LogMetadata](GetLogMetadata(componentId))

  def setLogLevel(componentId: ComponentId, level: Level): Future[Done] =
    postClient.requestResponse[Done](SetLogLevel(componentId, level))
}
