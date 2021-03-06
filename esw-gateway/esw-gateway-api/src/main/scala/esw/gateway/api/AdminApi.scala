package esw.gateway.api

import akka.Done
import csw.location.api.models.ComponentId
import csw.logging.models.{Level, LogMetadata}

import scala.concurrent.Future

trait AdminApi {

  /**
   * Fetches the LogMetadata for given component
   *
   * @param componentId   the component whose LogMetadata needs to be fetched
   * @return              a Future which completes and gives LogMetaData of the component
   */
  def getLogMetadata(componentId: ComponentId): Future[LogMetadata]

  /**
   * Updates the log level of component
   *
   * @param componentId   the component whose log level to be changed
   * @param level         represents log level to set
   */
  def setLogLevel(componentId: ComponentId, level: Level): Future[Done]
}
