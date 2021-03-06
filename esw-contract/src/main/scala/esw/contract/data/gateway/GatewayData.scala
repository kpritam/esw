package esw.contract.data.gateway

import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.location.api.models.{ComponentId, ComponentType}
import csw.logging.models.{Level, LogMetadata}
import csw.params.events.{EventKey, EventName, ObserveEvent, SystemEvent}
import csw.prefix.models.Subsystem
import esw.contract.data.sequencer.SequencerData
import esw.gateway.api.protocol.GatewayRequest._
import esw.gateway.api.protocol.GatewayStreamRequest.{Subscribe, SubscribeWithPattern}
import esw.gateway.api.protocol._

trait GatewayData extends SequencerData {
  val componentId: ComponentId = ComponentId(prefix, ComponentType.HCD)

  val eventName: EventName       = EventName("offline")
  val observeEvent: ObserveEvent = ObserveEvent(prefix, eventName)
  val systemEvent: SystemEvent   = SystemEvent(prefix, eventName)
  val eventKey: EventKey         = EventKey(prefix, eventName)

  val logMetadata: LogMetadata = LogMetadata(Level.INFO, Level.DEBUG, Level.INFO, Level.ERROR)

  val postComponentCommand: GatewayRequest.ComponentCommand = GatewayRequest.ComponentCommand(componentId, observeValidate)
  val postSequencerCommand: GatewayRequest.SequencerCommand = GatewayRequest.SequencerCommand(componentId, prepend)
  val publishEvent: PublishEvent                            = PublishEvent(observeEvent)
  val getEvent: GetEvent                                    = GetEvent(Set(eventKey))
  val alarmKey: AlarmKey                                    = AlarmKey(prefix, "someAlarm")
  val setAlarmSeverity: SetAlarmSeverity                    = SetAlarmSeverity(alarmKey, AlarmSeverity.Okay)
  val log: Log                                              = Log(prefix, Level.DEBUG, "message", Map("additional-info" -> 45))
  val setLogLevel: SetLogLevel                              = SetLogLevel(componentId, Level.ERROR)
  val getLogMetadata: GetLogMetadata                        = GetLogMetadata(componentId)

  val websocketComponentCommand: GatewayStreamRequest.ComponentCommand =
    GatewayStreamRequest.ComponentCommand(componentId, queryFinal)
  val websocketSequencerCommand: GatewayStreamRequest.SequencerCommand =
    GatewayStreamRequest.SequencerCommand(componentId, sequencerQueryFinal)
  val subscribe: Subscribe                       = Subscribe(Set(eventKey), Some(10))
  val subscribeWithPattern: SubscribeWithPattern = SubscribeWithPattern(Subsystem.CSW, Some(10), "[a-b]*")

  val invalidComponent: InvalidComponent               = InvalidComponent("invalid component")
  val emptyEventKeys: EmptyEventKeys                   = EmptyEventKeys()
  val eventServerUnavailable: EventServerUnavailable   = EventServerUnavailable()
  val invalidMaxFrequency: InvalidMaxFrequency         = InvalidMaxFrequency()
  val setAlarmSeverityFailure: SetAlarmSeverityFailure = SetAlarmSeverityFailure("alarm fail")
}
