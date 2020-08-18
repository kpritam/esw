package esw.agent.akka.client

import java.nio.file.Path

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.ComponentType.Machine
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Location}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentCommand.KillComponent
import esw.agent.akka.client.AgentCommand.SpawnCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.akka.client.AgentCommand.SpawnCommand.SpawnSelfRegistered.{SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.service.api.models._

import scala.concurrent.Future
import scala.jdk.DurationConverters.JavaDurationOps

class AgentClient(akkaLocation: AkkaLocation)(implicit actorSystem: ActorSystem[_]) {
  private implicit val timeout: Timeout = actorSystem.settings.config.getDuration("agent.akka.client.askTimeout").toScala

  private val agentRef: ActorRef[AgentCommand] = akkaLocation.uri.toActorRef.unsafeUpcast[AgentCommand]
  private val agentPrefix                      = akkaLocation.prefix

  def spawnSequenceComponent(componentName: String, version: Option[String] = None): Future[SpawnResponse] =
    agentRef ? (SpawnSequenceComponent(_, agentPrefix, componentName, version))

  def spawnSequenceManager(
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      version: Option[String] = None
  ): Future[SpawnResponse] =
    agentRef ? (SpawnSequenceManager(_, obsModeConfigPath, isConfigLocal, version))

  def spawnRedis(prefix: Prefix, port: Int, redisArguments: List[String]): Future[SpawnResponse] =
    agentRef ? (SpawnRedis(_, prefix, port, redisArguments))

  def killComponent(location: Location): Future[KillResponse] = agentRef ? (KillComponent(_, location))
}

object AgentClient {
  def make(agentPrefix: Prefix, locationService: LocationService)(implicit actorSystem: ActorSystem[_]): Future[AgentClient] = {
    import actorSystem.executionContext
    locationService
      .find(AkkaConnection(ComponentId(agentPrefix, Machine)))
      .map(_.getOrElse(throw AgentNotFoundException(s"could not resolve agent with prefix: $agentPrefix")))
      .map(new AgentClient(_))
  }
}
