package esw.gateway.server.routes

import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.{Directive, Directive0, Route, ValidationRejection}
import akka.stream.scaladsl.Source
import csw.event.api.scaladsl.SubscriptionModes.RateLimiterMode
import csw.event.api.scaladsl.{EventPublisher, EventSubscriber, EventSubscription}
import csw.params.core.formats.JsonSupport
import csw.params.core.models.Subsystem
import csw.params.events.{Event, EventKey}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import esw.template.http.server.commons.RichSourceExt.RichSource
import esw.template.http.server.csw.utils.CswContext

import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.language.postfixOps

class EventRoutes(cswCtx: CswContext) extends JsonSupport with PlayJsonSupport {
  import cswCtx._

  lazy val subscriber: EventSubscriber = eventService.defaultSubscriber
  lazy val publisher: EventPublisher   = eventService.defaultPublisher

  val route: Route = pathPrefix("event") {
    pathEnd {
      post {
        entity(as[Event]) { event =>
          complete(publisher.publish(event))
        }
      } ~
      get {
        parameter("key".as[String].*) { keys =>
          validateKeys(keys) {
            val eventualEvents = subscriber.get(keys.toEventKeys)
            complete(eventualEvents)
          }
        }
      }
    } ~
    pathPrefix("subscribe") {
      get {
        pathEnd {
          parameters(("key".as[String].*, "max-frequency".as[Int] ?)) { (keys, maxFrequency) =>
            validateKeys(keys) {
              validateFrequency(maxFrequency) {

                val events = maxFrequency match {
                  case Some(f) => subscriber.subscribe(keys.toEventKeys, maxFrequencyToDuration(f), RateLimiterMode)
                  case None    => subscriber.subscribe(keys.toEventKeys)
                }
                complete(events.toSSE)
              }
            }
          }
        } ~
        path(Segment) { subsystem =>
          val sub = Subsystem.withNameInsensitive(subsystem)
          parameters(("max-frequency".as[Int] ?, "pattern" ? "*")) { (maxFrequency, pattern) =>
            validateFrequency(maxFrequency) {

              val events: Source[Event, EventSubscription] = subscriber.pSubscribe(sub, pattern)
              val regulatedEvents = maxFrequency match {
                case Some(f) => events.via(eventSubscriberUtil.subscriptionModeStage(maxFrequencyToDuration(f), RateLimiterMode))
                case None    => events
              }
              complete(regulatedEvents.toSSE)
            }
          }
        }
      }
    }
  }

  private def validateKeys(keys: Iterable[String]): Directive0 = {
    validate(keys.nonEmpty, "Request is missing query parameter key")
  }

  private def validateFrequency(maxFrequency: Option[Int]): Directive0 = maxFrequency match {
    case Some(0) => reject(ValidationRejection("Max frequency should be greater than zero"))
    case _       => Directive.Empty
  }

  private def maxFrequencyToDuration(frequency: Int): FiniteDuration = (1000 / frequency).millis

  implicit class RichEventKeys(keys: Iterable[String]) {
    def toEventKeys: Set[EventKey] = keys.map(EventKey(_)).toSet
  }
}
