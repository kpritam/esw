package esw.ocs.dsl.highlevel

import akka.Done
import akka.actor.Cancellable
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventSubscriber
import csw.params.core.generics.Parameter
import csw.params.core.models.Prefix
import csw.params.events.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.util.*
import kotlin.time.Duration
import kotlin.time.toJavaDuration

data class Subscription(val cancel: suspend () -> Unit)

interface EventServiceDsl {
    val coroutineScope: CoroutineScope
    val defaultPublisher: IEventPublisher
    val defaultSubscriber: IEventSubscriber

    fun EventKey(prefix: String, eventName: String): EventKey = EventKey(Prefix(prefix), EventName(eventName))
    fun EventKey(eventKeyStr: String): EventKey = EventKey.apply(eventKeyStr)

    fun SystemEvent(sourcePrefix: String, eventName: String, vararg parameters: Parameter<*>): SystemEvent =
            SystemEvent(Prefix(sourcePrefix), EventName(eventName)).jMadd(parameters.toSet())

    fun ObserveEvent(sourcePrefix: String, eventName: String, vararg parameters: Parameter<*>): ObserveEvent =
            ObserveEvent(Prefix(sourcePrefix), EventName(eventName)).jMadd(parameters.toSet())

    suspend fun publishEvent(event: Event): Done = defaultPublisher.publish(event).await()

    fun publishEvent(every: Duration, eventGenerator: suspend CoroutineScope.() -> Event?): Cancellable =
            defaultPublisher.publishAsync({
                coroutineScope.future { Optional.ofNullable(eventGenerator()) }
            }, every.toJavaDuration())

    suspend fun onEvent(vararg eventKeys: String, callback: suspend CoroutineScope.(Event) -> Unit): Subscription {
        val subscription = defaultSubscriber.subscribeAsync(eventKeys.toEventKeys()) { coroutineScope.future { callback(it) } }
        subscription.ready().await()
        return Subscription { subscription.unsubscribe().await() }
    }

    suspend fun getEvent(vararg eventKeys: String): Set<Event> =
            defaultSubscriber.get(eventKeys.toEventKeys()).await().toSet()

    private fun (Array<out String>).toEventKeys(): Set<EventKey> = map { EventKey.apply(it) }.toSet()
}
