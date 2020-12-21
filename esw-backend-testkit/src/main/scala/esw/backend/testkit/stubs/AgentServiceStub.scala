package esw.backend.testkit.stubs

import java.nio.file.Path

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.backend.auth.MockedAuth
import csw.location.api.models.ComponentId
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.models.{KillResponse, Killed, SpawnResponse, Spawned}
import esw.agent.service.app.AgentServiceWiring
import esw.ocs.testkit.utils.LocationUtils

import scala.concurrent.Future

class AgentServiceStubImpl extends AgentServiceApi {
  override def spawnSequenceManager(
      agentPrefix: Prefix,
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      version: Option[String]
  ): Future[SpawnResponse] = Future.successful(Spawned)

  override def spawnSequenceComponent(
      agentPrefix: Prefix,
      componentName: String,
      version: Option[String]
  ): Future[SpawnResponse] = Future.successful(Spawned)

  override def spawnEventServer(
      agentPrefix: Prefix,
      sentinelConfPath: Path,
      port: Option[Int],
      version: Option[String]
  ): Future[SpawnResponse] = Future.successful(Spawned)

  override def spawnAlarmServer(
      agentPrefix: Prefix,
      sentinelConfPath: Path,
      port: Option[Int],
      version: Option[String]
  ): Future[SpawnResponse] = Future.successful(Spawned)

  override def spawnPostgres(
      agentPrefix: Prefix,
      pgDataConfPath: Path,
      port: Option[Int],
      dbUnixSocketDirs: String,
      version: Option[String]
  ): Future[SpawnResponse] = Future.successful(Spawned)

  override def killComponent(componentId: ComponentId): Future[KillResponse] = Future.successful(Killed)
}

class AgentServiceStub(val locationService: LocationService)(implicit val actorSystem: ActorSystem[SpawnProtocol.Command])
    extends LocationUtils {
  private var agentServiceWiring: Option[AgentServiceWiring] = _
  def spawnMockAgentService(): AgentServiceWiring = {
    println("hello")
    val wiring: AgentServiceWiring = new AgentServiceWiring() {
      override lazy val agentActorSystem: ActorSystem[SpawnProtocol.Command] = actorSystem
      override lazy val agentService: AgentServiceApi                        = new AgentServiceStubImpl()
      private val mockedAuth                                                 = new MockedAuth
      override private[esw] lazy val securityDirective                       = mockedAuth._securityDirectives
    }
    agentServiceWiring = Some(wiring)
    wiring.start()
    wiring
  }

  def shutdown(): Unit = {
    agentServiceWiring.foreach(_.stop().futureValue)
  }
}
