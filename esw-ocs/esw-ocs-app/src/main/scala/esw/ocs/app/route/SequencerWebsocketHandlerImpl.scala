package esw.ocs.app.route

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerWebsocketRequest
import esw.ocs.api.protocol.SequencerWebsocketRequest.QueryFinal
import msocket.api.MessageHandler
import msocket.impl.Encoding
import msocket.impl.ws.WebsocketStreamExtensions

class SequencerWebsocketHandlerImpl(adminApi: SequencerAdminApi, val encoding: Encoding[_])
    extends MessageHandler[SequencerWebsocketRequest, Source[Message, NotUsed]]
    with SequencerHttpCodecs
    with WebsocketStreamExtensions {

  override def handle(request: SequencerWebsocketRequest): Source[Message, NotUsed] = request match {
    case QueryFinal(sequenceId) => futureAsStream(adminApi.queryFinal(sequenceId))
  }
}
