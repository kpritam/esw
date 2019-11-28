package esw.ocs.dsl.script

import java.util.concurrent.CompletionStage
import java.util.function.Supplier

import akka.Done
import csw.params.commands.SequenceCommand
import csw.time.core.models.UTCTime

import scala.concurrent.Future

private[esw] class FSMScriptDsl(
    private val csw: CswServices,
    private val strandEc: StrandEc,
    private val initialState: FSMScriptState
) extends ScriptDsl(csw, strandEc) {

  def this(csw: CswServices, strandEc: StrandEc) = this(csw, strandEc, FSMScriptState.init())

  private var scriptState = initialState

  def become(nextState: String): Unit = {
    scriptState = scriptState.transition(nextState)
  }

  def add(state: String, script: Supplier[ScriptDsl]): Unit = {
    scriptState = scriptState.add(state, script)
  }

  override def execute(command: SequenceCommand): Future[Unit] =
    scriptState.currentScript.execute(command)

  override def executeGoOnline(): Future[Done] =
    scriptState.currentScript.executeGoOnline().flatMap(_ => super.executeGoOnline())

  override def executeGoOffline(): Future[Done] =
    scriptState.currentScript.executeGoOffline().flatMap(_ => super.executeGoOffline())

  override def executeShutdown(): Future[Done] =
    scriptState.currentScript.executeShutdown().flatMap(_ => super.executeShutdown())

  override def executeAbort(): Future[Done] =
    scriptState.currentScript.executeAbort().flatMap(_ => super.executeAbort())

  override def executeStop(): Future[Done] =
    scriptState.currentScript.executeStop().flatMap(_ => super.executeStop())

  override def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done] =
    scriptState.currentScript.executeDiagnosticMode(startTime, hint).flatMap(_ => super.executeDiagnosticMode(startTime, hint))

  override def executeOperationsMode(): Future[Done] =
    scriptState.currentScript.executeOperationsMode().flatMap(_ => super.executeOperationsMode())

  override def executeExceptionHandlers(ex: Throwable): CompletionStage[Void] =
    scriptState.currentScript.executeExceptionHandlers(ex).thenAccept(_ => super.executeExceptionHandlers(ex))

  // for testing purpose
  private[script] def getState = scriptState
}
