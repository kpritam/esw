package esw.gateway.server

import esw.template.http.server.CswContext
import esw.template.http.server.cli.{ArgsParser, Options}
import esw.template.http.server.http.HttpService

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object Main {
  def main(args: Array[String]): Unit = {
    new ArgsParser("http-server").parse(args).map {
      case Options(port) =>
        val cswContext = new CswContext(port)
        import cswContext._
        lazy val routes      = new Routes()
        lazy val httpService = new HttpService(locationService, routes.route, settings, actorRuntime)

        Await.result(httpService.registeredLazyBinding, 15.seconds)
    }
  }
}
