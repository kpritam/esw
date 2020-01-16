# Script Handlers

## Command Handlers

### onSetup

This handler is used to handle a @extref[Setup](csw:csw/params/commands/Setup.html) command sent to this sequencer.
The handler takes two parameters:

1. **command name** which is matched against the sequence command sent, if the command name matches, corresponding block provided
is executed
2. **block** of code which contains logic to act on the sequence command.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onSetup }

In the block provided to this handler, all the CSW services (Event, Alarm, Time Service, etc) and control DSL (loop, par etc) are accessible.


### onObserve

This handler is used to handle an @extref[Observe](csw:csw/params/commands/Observe.html) command sent to this sequencer.
The handler takes two parameters:

1. **command name** which is matched against the sequence command sent, if the command name matches, corresponding block provided
is executed
2. **block** of code which contains logic to act on the sequence command.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onObserve }


## Online and Offline Handlers

### onGoOnline

On receiving the `goOnline` command, the onGoOnline handlers, if defined, will be called. Only if the handlers execute successfully,
will the Sequencer become online. Hence, error handling for the block passed to onGoOnline needs to be taken care of by the script writer.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onGoOnline }


### onGoOffline

On receiving the `goOffline` command, the onGoOffline handlers, if defined, will be called. Only if the handlers execute successfully,
will the Sequencer become offline. Hence, error handling for the block passed to onGoOffline needs to be taken care of by the script writer.
Offline handlers could be written to clear the Sequencer state before going offline.

Kotlin
: @@snip [HandlersExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/HandlersExample.kts) { #onGoOffline }


## Abort Sequence Handler

## Stop Handler

## Shutdown Handler

## Diagnostic Mode Handler

## Operations Mode Handler

## exceptionHandlers