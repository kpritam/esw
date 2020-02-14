@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.core.generics.Key
import csw.params.core.models.Coords.Coord
import csw.params.core.models.JEqCoord
import esw.ocs.dsl.core.script
import esw.ocs.dsl.epics.CommandFlag
import esw.ocs.dsl.epics.EventVariable
import esw.ocs.dsl.epics.Fsm
import esw.ocs.dsl.params.Params
import esw.ocs.dsl.params.coordKey
import esw.ocs.dsl.params.intKey
import esw.ocs.dsl.params.params
import kotlin.time.milliseconds
import kotlin.time.seconds

script {

    //#create-fsm
    val irisFsm: Fsm = Fsm(name = "iris-fsm", initState = "INIT") {
        // place to define all states of FSM
    }
    //#create-fsm

    //#start-fsm
    irisFsm.start()
    //#start-fsm

    //#await
    irisFsm.await()
    //#await


    //#event-var
    //#polling
    val tempKey: Key<Int> = intKey("temperature")
    //#polling
    val eventVariable: EventVariable<Int> = EventVariable(0, "esw.temperature.temp", tempKey)

    eventVariable.bind(irisFsm) // binds the FSM and event variable

    eventVariable.getParam() // to get the current values of the parameter

    eventVariable.first() // to get the first value from the values of the parameter

    eventVariable.setParam(10) // publishes the given value on event key
    //#event-var

    //#polling
    val pollingSysVar: EventVariable<Int> = EventVariable(0, "esw.temperature.temp", tempKey, 2.seconds)
    //#polling

    var params = Params(mutableSetOf())

    //#command-flag
    val flag = CommandFlag()
    flag.bind(irisFsm) // bind the FSM and command flag

    onSetup("setup-command") { command ->
        flag.set(command.params) // will set params and refreshes the bound FSMs with the new params
    }

    flag.value() // way to extract the current params value in FSM
    //#command-flag

    val exampleFsm = Fsm(name = "example-fsm", initState = "INIT") {

        val condition = true

        //#define-state
        state("INIT") {
            // actions to be performed in this state
        }
        //#define-state

        state("BECOME-STATE") {

            //#entry
            entry {
                // do something
            }
            //#entry

            //#after
            after(100.milliseconds) {
                // do something
            }
            //#after

            //#state-transition
            become(state = "IN-PROGRESS")
            //#state-transition

            //#complete-fsm
            completeFsm()   // will complete the Fsm
            // anything after this will not be executed
            //#complete-fsm
        }

        val temparature = eventVariable

        //#state-transition-on-re-evaluation
        state("LOW") {
            //#on
            on(temparature.first() < 20) {
                // do something but state transition does not happen
            }

            on(temparature.first() >= 20) {
                // do something and transit state
                become("HIGH")
            }
            //#on
        }
        //#state-transition-on-re-evaluation
    }
}
