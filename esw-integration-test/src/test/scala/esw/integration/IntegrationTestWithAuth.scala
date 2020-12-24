package esw.integration

import akka.actor.CoordinatedShutdown.UnknownReason
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.{ComponentId, ComponentType, TcpLocation}
import csw.logging.client.scaladsl.LoggingSystemFactory
import esw.agent.akka.AgentSetup
import esw.agent.akka.app.AgentSettings
import esw.agent.akka.app.process.cs.Coursier
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.client.AgentServiceClientFactory
import esw.agent.service.api.models.Spawned
import esw.agent.service.app.{AgentServiceApp, AgentServiceWiring}
import esw.constants.AgentConstants
import esw.gateway.server.{GatewaySetup, GatewayWiring}
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.AAS
import esw.sm.app.SequenceManagerSetup

import java.nio.file.Path
import scala.concurrent.duration.DurationLong
import scala.util.control.NonFatal

class IntegrationTestWithAuth extends EswTestKit(AAS) with GatewaySetup with AgentSetup with SequenceManagerSetup {
  var gatewayServerWiring: GatewayWiring               = _
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.minute, 100.millis)
  private var agentService: AgentServiceApi            = _
  private var agentServiceWiring: AgentServiceWiring   = _

  LoggingSystemFactory.start("Test", "0.1.0-SNAPSHOT", "localhost", actorSystem)

  override def beforeAll(): Unit = {
    // gateway setup
    super.beforeAll()
    gatewayServerWiring = startGateway()
    // agent app setup
    spawnAgent(AgentSettings(agentPrefix, 1.minute, channel))
    // agent service setup
    agentServiceWiring = AgentServiceApp.start(startLogging = false)
    val httpLocation = resolveHTTPLocation(agentServiceWiring.prefix, ComponentType.Service)
    agentService = AgentServiceClientFactory(httpLocation, () => tokenWithEswUserRole())
    Coursier.locationAgentApp(Some("713742785d")).fetch("https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json")
  }

  override def afterAll(): Unit = {
    gatewayServerWiring.actorRuntime.shutdown(UnknownReason).futureValue
    locationService.unregisterAll()
    agentServiceWiring.stop().futureValue
    super.afterAll()
  }

  "AgentService" must {

    "spawn and kill Database Server on a given agent | ESW-368" in {

      val pgDataConfPath            = ResourceReader.copyToTmp("pg_hba.conf")
      val dbUnixSocketDirs          = "/tmp"
      val postgresServerComponentID = ComponentId(AgentConstants.databasePrefix, Service)
      val postgresServerConnection  = TcpConnection(postgresServerComponentID)

      try {
        val spawnResponse = agentService
          .spawnPostgres(
            agentPrefix,
            Path.of(pgDataConfPath.getAbsolutePath),
            Some(8085),
            dbUnixSocketDirs,
            Some("713742785d")
          )
          .futureValue
        spawnResponse should ===(Spawned)

        // verify registration in location service
        val eventServerLocation: TcpLocation = locationService.resolve(postgresServerConnection, 5.seconds).futureValue.value

        // verify agent prefix and pid metadata is present in Sequence component akka location
        eventServerLocation.metadata.getAgentPrefix.get should ===(agentPrefix)
        eventServerLocation.metadata.getPid.isDefined should ===(true)

        agentService.killComponent(postgresServerComponentID).futureValue

      }
      catch {
        case NonFatal(e) =>
          println(
            "Make sure 'PGDATA' env variable is set where postgres database is installed e.g. for mac: /usr/local/var/postgres"
          )
          throw e
      }

    }
  }

}
