package esw.agent.akka.client

import java.nio.file.Path

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.ComponentType.Machine
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Location}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW
import esw.agent.akka.client.AgentCommand.KillComponent
import esw.agent.akka.client.AgentCommand.SpawnCommand.{SpawnRedis, SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.service.api.models._
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.constants.AgentTimeouts

import scala.concurrent.Future

class AgentClient(akkaLocation: AkkaLocation)(implicit actorSystem: ActorSystem[_]) {
  private val agentRef: ActorRef[AgentCommand] = akkaLocation.uri.toActorRef.unsafeUpcast[AgentCommand]
  private val agentPrefix                      = akkaLocation.prefix

  def spawnEventServer(confPath: String, port: Option[Int], version: Option[String]): Future[SpawnResponse] =
    (agentRef ? (SpawnRedis(_, Prefix(CSW, "EventServer"), confPath, port, version)))(
      AgentTimeouts.SpawnComponent,
      actorSystem.scheduler
    )

  def spawnAlarmServer(confPath: String, port: Option[Int], version: Option[String]): Future[SpawnResponse] =
    (agentRef ? (SpawnRedis(_, Prefix(CSW, "AlarmServer"), confPath, port, version)))(
      AgentTimeouts.SpawnComponent,
      actorSystem.scheduler
    )

  def spawnSequenceComponent(componentName: String, version: Option[String] = None): Future[SpawnResponse] =
    (agentRef ? (SpawnSequenceComponent(_, agentPrefix, componentName, version)))(
      AgentTimeouts.SpawnComponent,
      actorSystem.scheduler
    )

  def spawnSequenceManager(
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      version: Option[String] = None
  ): Future[SpawnResponse] =
    (agentRef ? (SpawnSequenceManager(_, obsModeConfigPath, isConfigLocal, version)))(
      AgentTimeouts.SpawnComponent,
      actorSystem.scheduler
    )

  def killComponent(location: Location): Future[KillResponse] =
    (agentRef ? (KillComponent(_, location)))(AgentTimeouts.KillComponent, actorSystem.scheduler)
}

object AgentClient {

  //create AgentClient(Actor client or proxy) for the agent of the given prefix
  //if there is no agent running with the given prefix it returns the FindLocationError as a Future
  //else AgentClient gets returned as a Future
  def make(agentPrefix: Prefix, locationService: LocationServiceUtil)(implicit
      actorSystem: ActorSystem[_]
  ): Future[Either[EswLocationError.FindLocationError, AgentClient]] = {
    import actorSystem.executionContext
    locationService
      .find(AkkaConnection(ComponentId(agentPrefix, Machine)))
      .mapRight(new AgentClient(_))
  }
}
