package esw.integration

import java.io.File
import java.nio.file.{Files, Path, Paths}

import akka.actor.CoordinatedShutdown.UnknownReason
import csw.config.api.scaladsl.ConfigService
import csw.config.api.{ConfigData, TokenFactory}
import csw.config.client.scaladsl.ConfigClientFactory
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer, Service}
import csw.location.api.models.Connection.{AkkaConnection, TcpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType, Metadata, TcpLocation}
import csw.params.commands.CommandResponse.Started
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem._
import csw.testkit.ConfigTestKit
import esw.agent.akka.AgentSetup
import esw.agent.akka.app.AgentSettings
import esw.agent.akka.app.process.cs.Coursier
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.client.AgentServiceClientFactory
import esw.agent.service.api.models.{Killed, Spawned}
import esw.agent.service.app.{AgentServiceApp, AgentServiceWiring}
import esw.commons.utils.location.LocationServiceUtil
import esw.constants.AgentConstants
import esw.gateway.api.clients.ClientFactory
import esw.gateway.server.{GatewaySetup, GatewayWiring}
import esw.ocs.api.actor.client.{SequenceComponentImpl, SequencerImpl}
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.SequenceComponentResponse.SequencerLocation
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.AAS
import esw.sm.api.models.{AgentStatus, ProvisionConfig, SequenceComponentStatus}
import esw.sm.api.protocol.CommonFailure.{ConfigurationMissing, LocationServiceError}
import esw.sm.api.protocol.ConfigureResponse.ConflictingResourcesWithRunningObsMode
import esw.sm.api.protocol.StartSequencerResponse.{LoadScriptError, SequenceComponentNotAvailable}
import esw.sm.api.protocol._
import esw.sm.app.TestSetup.obsModeConfigPath
import esw.sm.app.{SequenceManagerApp, SequenceManagerSetup, TestSetup}
import msocket.http.HttpError

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong
import scala.util.control.NonFatal

class IntegrationTestWithAuth extends EswTestKit(AAS) with GatewaySetup with AgentSetup with SequenceManagerSetup {
  var gatewayServerWiring: GatewayWiring               = _
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(1.minute, 100.millis)
  private var agentClient: AgentClient                 = _
  private var agentService: AgentServiceApi            = _
  private var agentServiceWiring: AgentServiceWiring   = _
  private val locationServiceUtil                      = new LocationServiceUtil(locationService)
  private val configTestKit: ConfigTestKit             = frameworkTestKit.configTestKit

  override def beforeAll(): Unit = {
    // gateway setup
    super.beforeAll()
    gatewayServerWiring = startGateway()
    // agent app setup
    spawnAgent(AgentSettings(agentPrefix, 1.minute, channel))
    agentClient = AgentClient.make(agentPrefix, locationServiceUtil).rightValue
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
