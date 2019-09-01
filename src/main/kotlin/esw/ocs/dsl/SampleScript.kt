package esw.ocs.dsl

import csw.params.commands.CommandResponse
import csw.params.core.models.Prefix
import csw.params.events.EventName
import csw.params.events.SystemEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

open class SampleScript(cswServices: CswServices) : ScriptKt(cswServices) {
    init {
        val eventKey = "csw.a.b."
        fun event(id: Int) = SystemEvent(Prefix("csw.a.b"), EventName(id.toString()))

        handleSetup("command-1") { command ->
            log("============ command-1 ================")

            repeat(50) {
                async {
                    log("Publishing event $it")
                    delay(1000)
                    publishEvent(event(it))
                    log("Published event $it")
                }
            }

            log("============ command-1 -End ================")
            cswServices.crm().addOrUpdateCommand(CommandResponse.Completed(command.runId()))
        }

        handleSetup("command-2") { command ->
            log("============ command-2 ================")
            val events = getEvent(eventKey + 1)
            log(events.toString())
            events.forEach(::println)
            cswServices.crm().addOrUpdateCommand(CommandResponse.Completed(command.runId()))
        }

        handleSetup("command-3") { command ->
            log("============ command-3 ================")

            val keys = (49.downTo(0)).map { eventKey + it }.toTypedArray()

            onEvent(*keys) { event ->
                println("=======================")
                log("Received: ${event.eventName()}")
            }

            cswServices.crm().addOrUpdateCommand(CommandResponse.Completed(command.runId()))
        }
    }

}