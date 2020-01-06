package esw.ocs.scripts.examples.paradox

import csw.params.core.models.JEqCoord
import esw.ocs.dsl.core.script
import esw.ocs.dsl.epics.CommandFlag
import esw.ocs.dsl.params.Params
import esw.ocs.dsl.params.coordKey
import esw.ocs.dsl.params.intKey
import kotlin.time.milliseconds

script {

    //#create-fsm
    val irisFsm = Fsm(name = "iris-fsm", initState = "INIT") {
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
    val tempKey = intKey("temperature")
    val systemVar = SystemVar(0, "esw.temperature.temp", tempKey)

    systemVar.bind(irisFsm) // binds the FSM and event variable

    val coordKey = coordKey("co-ordinates")
    val observeVar = ObserveVar(JEqCoord.make(0, 0), "iris.observe.coord", coordKey)

    observeVar.bind(irisFsm) // binds the FSM and event variable
    //#event-var

    var params = Params(mutableSetOf())

    //#command-flag
    val flag = CommandFlag()
    flag.bind(irisFsm) // bind the FSM and command flag

    flag.set(params) //refreshes the bound FSMs with the new params.
    //#command-flag

    val exampleFsm = Fsm(name = "example-fsm", initState = "INIT") {

        val condition = true

        //#define-state
        state("INIT") {
            // actions to be performed in this state
        }
        //#define-state

        state("BECOME-STATE") {

            //#on
            on(condition) {
                // executes this when condition is true
            }

            on {
                // executes this every time
            }
            //#on

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
            // will complete the Fsm
            completeFsm()
            // anything after this will not be executed
            //#complete-fsm
        }
    }
}
