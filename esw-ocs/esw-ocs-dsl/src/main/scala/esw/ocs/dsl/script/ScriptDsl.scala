package esw.ocs.dsl.script

import java.util.Optional
import java.util.concurrent.{CompletableFuture, CompletionStage}
import java.util.function.Supplier

import akka.Done
import csw.params.commands.{Observe, SequenceCommand, Setup}
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol.PullNextResult
import esw.ocs.dsl.script.exceptions.UnhandledCommandException
import esw.ocs.dsl.script.utils.{FunctionBuilder, FunctionHandlers}

import scala.async.Async.{async, await}
import scala.compat.java8.FutureConverters.{CompletionStageOps, FutureOps}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

private[esw] class ScriptDsl(val csw: CswServices, strandEc: StrandEc) {
  protected implicit lazy val toEc: ExecutionContext = strandEc.ec

  var isOnline = true

  private val commandHandlerBuilder: FunctionBuilder[SequenceCommand, CompletionStage[Void]] = new FunctionBuilder

  private val onlineHandlers: FunctionHandlers[Unit, CompletionStage[Void]]                  = new FunctionHandlers
  private val offlineHandlers: FunctionHandlers[Unit, CompletionStage[Void]]                 = new FunctionHandlers
  private val shutdownHandlers: FunctionHandlers[Unit, CompletionStage[Void]]                = new FunctionHandlers
  private val abortHandlers: FunctionHandlers[Unit, CompletionStage[Void]]                   = new FunctionHandlers
  private val stopHandlers: FunctionHandlers[Unit, CompletionStage[Void]]                    = new FunctionHandlers
  private val diagnosticHandlers: FunctionHandlers[(UTCTime, String), CompletionStage[Void]] = new FunctionHandlers
  private val operationsHandlers: FunctionHandlers[Unit, CompletionStage[Void]]              = new FunctionHandlers
  private val exceptionHandlers: FunctionHandlers[Throwable, CompletionStage[Void]]          = new FunctionHandlers

  def merge(that: ScriptDsl): ScriptDsl = {
    commandHandlerBuilder ++ that.commandHandlerBuilder
    onlineHandlers ++ that.onlineHandlers
    offlineHandlers ++ that.offlineHandlers
    shutdownHandlers ++ that.shutdownHandlers
    abortHandlers ++ that.abortHandlers
    stopHandlers ++ that.stopHandlers
    diagnosticHandlers ++ that.diagnosticHandlers
    operationsHandlers ++ that.operationsHandlers
    exceptionHandlers ++ that.exceptionHandlers
    this
  }

  private def handle[T <: SequenceCommand: ClassTag](name: String)(handler: T => CompletionStage[Void]): Unit =
    commandHandlerBuilder.addHandler[T](handler)(_.commandName.name == name)

  private lazy val commandHandler: SequenceCommand => CompletionStage[Void] =
    commandHandlerBuilder.build { input =>
      // fixme: should script writer have ability to add this default handler, like handleUnknownCommand
      CompletableFuture.failedFuture(new UnhandledCommandException(input))
    }

  def execute(command: SequenceCommand): Future[Unit] = commandHandler(command).toScala.map(_ => ())

  private def executeHandler[T](f: FunctionHandlers[T, CompletionStage[Void]], arg: T): Future[Unit] =
    Future.sequence(f.execute(arg).map(_.toScala)).map(_ => ())

  def executeGoOnline(): Future[Done] =
    executeHandler(onlineHandlers, ()).map { _ =>
      isOnline = true
      Done
    }

  def executeGoOffline(): Future[Done] = {
    isOnline = false
    executeHandler(offlineHandlers, ()).map(_ => Done)
  }

  def executeShutdown(): Future[Done] = executeHandler(shutdownHandlers, ()).map(_ => Done)

  def executeAbort(): Future[Done] = executeHandler(abortHandlers, ()).map(_ => Done)

  def executeStop(): Future[Done] = executeHandler(stopHandlers, ()).map(_ => Done)

  def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done] =
    Future.sequence(diagnosticHandlers.execute((startTime, hint)).map(_.toScala)).map(_ => Done)

  def executeOperationsMode(): Future[Done] = executeHandler(operationsHandlers, ()).map(_ => Done)

  def executeExceptionHandlers(ex: Throwable): CompletionStage[Void] =
    executeHandler(exceptionHandlers, ex).toJava.thenAccept(_ => ())

  protected final def nextIf(f: SequenceCommand => Boolean): CompletionStage[Optional[SequenceCommand]] =
    async {
      val operator  = csw.sequenceOperatorFactory()
      val mayBeNext = await(operator.maybeNext)
      mayBeNext match {
        case Some(step) if f(step.command) =>
          await(operator.pullNext) match {
            case PullNextResult(step) => Optional.ofNullable(step.command)
            case _                    => Optional.empty[SequenceCommand]
          }
        case _ => Optional.empty[SequenceCommand]
      }
    }.toJava

  protected final def onSetupCommand(name: String)(handler: Setup => CompletionStage[Void]): Unit =
    handle[Setup](name)(handler(_))

  protected final def onObserveCommand(name: String)(handler: Observe => CompletionStage[Void]): Unit =
    handle[Observe](name)(handler(_))

  protected final def onGoOnline(handler: Supplier[CompletionStage[Void]]): Unit      = onlineHandlers.add(_ => handler.get())
  protected final def onAbortSequence(handler: Supplier[CompletionStage[Void]]): Unit = abortHandlers.add(_ => handler.get())
  protected final def onStop(handler: Supplier[CompletionStage[Void]]): Unit          = stopHandlers.add(_ => handler.get())
  protected final def onShutdown(handler: Supplier[CompletionStage[Void]]): Unit      = shutdownHandlers.add(_ => handler.get())
  protected final def onGoOffline(handler: Supplier[CompletionStage[Void]]): Unit     = offlineHandlers.add(_ => handler.get())
  protected final def onDiagnosticMode(handler: (UTCTime, String) => CompletionStage[Void]): Unit =
    diagnosticHandlers.add((x: (UTCTime, String)) => handler(x._1, x._2))
  protected final def onOperationsMode(handler: Supplier[CompletionStage[Void]]): Unit =
    operationsHandlers.add(_ => handler.get())

  protected final def onException(handler: Throwable => CompletionStage[Void]): Unit = exceptionHandlers.add(handler)
}