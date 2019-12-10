package esw.ocs.testkit

import akka.actor
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.sequencer.SequencerMsg
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.location.api.extensions.URIExtension._
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.core.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.{CSWService, ScalaTestFrameworkTestKit}
import esw.ocs.api.SequencerApi
import esw.ocs.api.protocol.ScriptError
import esw.ocs.app.wiring.{SequenceComponentWiring, SequencerWiring}
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.{SequenceComponentImpl, SequencerActorProxy, SequencerApiFactory}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong

abstract class EswTestKit(services: CSWService*) extends ScalaTestFrameworkTestKit(services: _*) with BaseTestSuite {
  implicit lazy val system: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem
  implicit lazy val ec: ExecutionContext                       = frameworkTestKit.frameworkWiring.actorRuntime.ec
  implicit lazy val askTimeout: Timeout                        = Timeout(10.seconds)
  lazy val locationService: LocationService                    = frameworkTestKit.frameworkWiring.locationService
  lazy val untypedSystem: actor.ActorSystem                    = frameworkTestKit.frameworkWiring.actorRuntime.classicSystem

  private lazy val eventService: EventService = frameworkTestKit.frameworkWiring.eventServiceFactory.make(locationService)
  lazy val eventSubscriber: EventSubscriber   = eventService.defaultSubscriber
  lazy val eventPublisher: EventPublisher     = eventService.defaultPublisher

  private val sequenceComponentLocations: mutable.Buffer[AkkaLocation] = mutable.Buffer.empty

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  override def afterAll(): Unit = {
    shutdownAllSequencers()
    super.afterAll()
  }

  def clearAll(): Unit = {
    sequenceComponentLocations.clear()
  }

  def shutdownAllSequencers(): Unit = {
    sequenceComponentLocations.foreach(x => new SequenceComponentImpl(x.uri.toActorRef.unsafeUpcast).unloadScript())
    clearAll()
  }

  def spawnSequencerRef(subsystem: Subsystem, observingMode: String): ActorRef[SequencerMsg] =
    spawnSequencer(subsystem, observingMode).rightValue.sequencerRef

  def spawnSequencerProxy(subsystem: Subsystem, observingMode: String) =
    new SequencerActorProxy(spawnSequencerRef(subsystem, observingMode))

  def spawnSequencer(subsystem: Subsystem, observingMode: String): Either[ScriptError, AkkaLocation] = {
    val sequenceComponent = spawnSequenceComponent(subsystem, None)
    val locationE = sequenceComponent.flatMap { seqCompLocation =>
      new SequenceComponentImpl(seqCompLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg])
        .loadScript(subsystem, observingMode)
        .futureValue
        .response
    }
    locationE.left.foreach(println) // this is to print the exception in case script loading fails
    locationE
  }

  def spawnSequenceComponent(subsystem: Subsystem, name: Option[String]): Either[ScriptError, AkkaLocation] = {
    val wiring = new SequenceComponentWiring(subsystem, name, new SequencerWiring(_, _, _).sequencerServer)
    wiring.start().map { seqCompLocation =>
      sequenceComponentLocations += seqCompLocation
      seqCompLocation
    }
  }

  private def resolveSequencerHttp(subsystem: Subsystem, observingMode: String) = {
    val componentId = ComponentId(Prefix(subsystem, observingMode), ComponentType.Sequencer)
    locationService.resolve(HttpConnection(componentId), 5.seconds).futureValue.get
  }

  def sequencerClient(subsystem: Subsystem, observingMode: String): SequencerApi = {
    val httpLocation = resolveSequencerHttp(subsystem, observingMode)
    SequencerApiFactory.make(httpLocation)
  }

  def resolveSequencerLocation(prefix: Prefix): AkkaLocation =
    resolve(prefix, ComponentType.Sequencer)

  def resolveSequencerLocation(subsystem: Subsystem, observingMode: String): AkkaLocation =
    resolveSequencerLocation(Prefix(subsystem, observingMode))

  def resolveSequencer(subsystem: Subsystem, observingMode: String): ActorRef[SequencerMsg] =
    resolveSequencerLocation(subsystem, observingMode).uri.toActorRef
      .unsafeUpcast[SequencerMsg]

  def resolveSequenceComponentLocation(prefix: Prefix): AkkaLocation =
    resolve(prefix, ComponentType.SequenceComponent)

  private def resolve(prefix: Prefix, componentType: ComponentType) =
    locationService
      .resolve(AkkaConnection(ComponentId(prefix, componentType)), 5.seconds)
      .futureValue
      .value

  def resolveSequenceComponent(prefix: Prefix): ActorRef[SequenceComponentMsg] =
    resolveSequenceComponentLocation(prefix).uri.toActorRef
      .unsafeUpcast[SequenceComponentMsg]
}
