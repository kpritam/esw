package esw.ocs.scripts.examples.epics

import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay

script {
    val fsm = FSM("Trigger FSM") {
        state("INIT") {
            println("INIT state")
            delay(1000)
            publishEvent(SystemEvent("tcs", "trigger.INIT.state"))
            on(true) {
                become("READY")
            }
        }

        state("READY") {
            publishEvent(SystemEvent("tcs", "trigger.READY.state"))
            become("DONE")
        }

        state("DONE") {
            publishEvent(SystemEvent("tcs", "trigger.DONE.state"))
            completeFsm()
        }
    }

    onSetup("command-1") { command1 ->
        fsm.start("INIT")
        fsm.await()
    }
}