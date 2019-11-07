package esw.ocs.dsl.core

import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Observe
import csw.params.commands.Sequence
import csw.params.commands.SequenceCommand
import csw.params.commands.Setup
import csw.time.core.models.UTCTime
import esw.ocs.dsl.highlevel.CswHighLevelDsl
import esw.ocs.dsl.nullable
import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.JScriptDsl
import esw.ocs.dsl.script.StrandEc
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future

sealed class ScriptDslKt(private val cswServices: CswServices) : CswHighLevelDsl(cswServices) {

    // https://stackoverflow.com/questions/58497383/is-it-possible-to-provide-custom-name-for-internal-delegated-properties-in-kotli/58497535#58497535
    @get:JvmName("scriptDsl")
    internal val scriptDsl: JScriptDsl by lazy { ScriptDslFactory.make(cswServices, strandEc) }

    suspend fun nextIf(predicate: (SequenceCommand) -> Boolean): SequenceCommand? =
            scriptDsl.nextIf { predicate(it) }.await().nullable()

    fun finishWithError(message: String = ""): Nothing = throw RuntimeException(message)

    fun handleSetup(name: String, block: suspend CoroutineScope.(Setup) -> Unit) =
            scriptDsl.handleSetupCommand(name) { block.toJavaFuture(it) }

    fun handleObserve(name: String, block: suspend CoroutineScope.(Observe) -> Unit) =
            scriptDsl.handleObserveCommand(name) { block.toJavaFuture(it) }

    fun handleGoOnline(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.handleGoOnline { block.toJavaFutureVoid() }

    fun handleGoOffline(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.handleGoOffline { block.toJavaFutureVoid() }

    fun handleAbortSequence(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.handleAbortSequence { block.toJavaFutureVoid() }

    fun handleShutdown(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.handleShutdown { block.toJavaFutureVoid() }

    fun handleDiagnosticMode(block: suspend (UTCTime, String) -> Unit) =
            scriptDsl.handleDiagnosticMode { x: UTCTime, y: String ->
                coroutineScope.launch { block(x, y) }.asCompletableFuture().thenAccept { }
            }

    fun handleOperationsMode(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.handleOperationsMode { block.toJavaFutureVoid() }

    fun handleStop(block: suspend CoroutineScope.() -> Unit) =
            scriptDsl.handleStop { block.toJavaFutureVoid() }

    fun handleException(block: suspend CoroutineScope.(Throwable) -> Unit) =
            scriptDsl.handleException {
                // "future" is used to swallow the exception coming from exception handlers
                coroutineScope.future { block(it) }
                        .exceptionally { log("Exception is thrown from Exception handler with message : ${it.message}") }
                        .thenAccept { }
            }

    fun loadScripts(vararg reusableScriptResult: ReusableScriptResult) =
            reusableScriptResult.forEach {
                this.scriptDsl.merge(it(cswServices, strandEc, coroutineScope).scriptDsl)
            }

    suspend fun submitSequence(sequencerName: String, observingMode: String, sequence: Sequence): SubmitResponse =
            this.scriptDsl.submitSequence(sequencerName, observingMode, sequence).await()

    // fixme: use logging service
    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

}

class ReusableScript(
        cswServices: CswServices,
        override val strandEc: StrandEc,
        override val coroutineScope: CoroutineScope
) : ScriptDslKt(cswServices)


open class Script(cswServices: CswServices) : ScriptDslKt(cswServices) {
    private val _strandEc = StrandEc.apply()
    private val supervisorJob = SupervisorJob()
    private val dispatcher = _strandEc.executorService().asCoroutineDispatcher()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        log("Exception thrown in script with message: ${exception.message}")
        scriptDsl.executeExceptionHandlers(exception)
    }

    override val coroutineScope: CoroutineScope get() = CoroutineScope(supervisorJob + dispatcher + exceptionHandler)
    override val strandEc: StrandEc get() = _strandEc

    // fixme: call me when shutting down sequencer
    fun close() {
        supervisorJob.cancel()
        dispatcher.close()
    }
}
