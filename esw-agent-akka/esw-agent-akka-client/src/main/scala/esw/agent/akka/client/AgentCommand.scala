package esw.agent.akka.client

import java.nio.file.Path

import akka.actor.typed.ActorRef
import csw.location.api.models.ComponentType.{SequenceComponent, Service}
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.api.models._
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.service.api._
import esw.agent.service.api.models.{KillResponse, SpawnResponse}

sealed trait AgentCommand
sealed trait AgentRemoteCommand extends AgentCommand with AgentAkkaSerializable

object AgentCommand {

  sealed trait SpawnCommand extends AgentRemoteCommand {
    def replyTo: ActorRef[SpawnResponse]
    def commandArgs(extraArgs: List[String] = List.empty): List[String]
    def prefix: Prefix
    def connection: Connection

    def componentId: ComponentId = connection.componentId
  }

  object SpawnCommand {

    case class SpawnAAS(
        replyTo: ActorRef[SpawnResponse],
        prefix: Prefix,
        keycloakDir: Path,
        migrationFilePath: Path,
        port: Option[Int],
        version: Option[String]
    ) extends SpawnCommand {

      override def commandArgs(extraArgs: List[String]): List[String] = {
        def commandWithPort(port: Int): String = command + s" -Djboss.http.port=${port}"
        def command =
          s"$keycloakDir/bin/standalone.sh -Dkeycloak.migration.action=import -Dkeycloak.migration.provider=singleFile -Dkeycloak.migration.file=$migrationFilePath"

        port match {
          case Some(value) =>
            List(
              "--prefix",
              prefix.toString(),
              "--http",
              "auth",
              "--command",
              commandWithPort(value),
              "--port",
              value.toString
            ) ::: extraArgs
          case None =>
            List("--prefix", prefix.toString(), "--http", "auth", "--command", command) ::: extraArgs
        }
      }

      override def connection: Connection = HttpConnection(ComponentId(prefix, Service))

    }

    case class SpawnRedis(
        replyTo: ActorRef[SpawnResponse],
        prefix: Prefix,
        confPath: Path,
        port: Option[Int],
        version: Option[String]
    ) extends SpawnCommand {
      override def commandArgs(extraArgs: List[String]): List[String] = {
        def command(port: Int) = s"redis-sentinel $confPath --port $port"
        port match {
          case Some(value) =>
            List("--prefix", prefix.toString(), "--command", command(value), "--port", value.toString) ::: extraArgs
          case None => List("--prefix", prefix.toString(), "--command", s"redis-sentinel $confPath") ::: extraArgs
        }
      }

      override def connection: Connection = TcpConnection(ComponentId(prefix, Service))
    }

    case class SpawnPostgres(
        replyTo: ActorRef[SpawnResponse],
        prefix: Prefix,
        pgDataConfPath: Path,
        port: Option[Int],
        dbUnixSocketDirs: String,
        version: Option[String]
    ) extends SpawnCommand {
      override def commandArgs(extraArgs: List[String]): List[String] = {
        def command = s"postgres --hba_file=$pgDataConfPath --unix_socket_directories=$dbUnixSocketDirs"
        (port match {
          case Some(value) =>
            List("--prefix", prefix.toString(), "--command", command + s" -i -p $value", "--port", value.toString)
          case None => List("--prefix", prefix.toString(), "--command", command)
        }) ::: extraArgs
      }

      override def connection: Connection = TcpConnection(ComponentId(prefix, Service))
    }

    case class SpawnSequenceComponent(
        replyTo: ActorRef[SpawnResponse],
        agentPrefix: Prefix,
        componentName: String,
        version: Option[String]
    ) extends SpawnCommand {
      override val prefix: Prefix             = Prefix(agentPrefix.subsystem, componentName)
      override val connection: AkkaConnection = AkkaConnection(ComponentId(prefix, SequenceComponent))
      override def commandArgs(extraArgs: List[String]): List[String] =
        List("seqcomp", "-s", prefix.subsystem.name, "-n", componentName) ++ extraArgs
    }

    case class SpawnSequenceManager(
        replyTo: ActorRef[SpawnResponse],
        obsModeConfigPath: Path,
        isConfigLocal: Boolean,
        version: Option[String]
    ) extends SpawnCommand {
      override val prefix: Prefix             = Prefix(ESW, "sequence_manager")
      override val connection: AkkaConnection = AkkaConnection(ComponentId(prefix, Service))
      private val command                     = List("start", "-o", obsModeConfigPath.toString)
      override def commandArgs(extraArgs: List[String]): List[String] = {
        val args = if (isConfigLocal) command :+ "-l" else command
        args ++ extraArgs
      }
    }
  }

  case class KillComponent(replyTo: ActorRef[KillResponse], location: Location) extends AgentRemoteCommand
}
