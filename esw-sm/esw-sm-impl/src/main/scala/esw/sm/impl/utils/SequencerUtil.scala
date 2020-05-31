package esw.sm.impl.utils

import akka.Done
import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation, Location}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.Timeouts
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.commons.utils.FutureUtils
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.{SequenceComponentApi, SequencerApi}
import esw.sm.api.models.ConfigureResponse.{FailedToStartSequencers, Success}
import esw.sm.api.models.{ConfigureResponse, SequenceManagerError, SequencerError}
import esw.sm.impl.config.Sequencers

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class SequencerUtil(locationServiceUtil: LocationServiceUtil, sequenceComponentUtil: SequenceComponentUtil)(implicit
    actorSystem: ActorSystem[_]
) {
  implicit private val ec: ExecutionContext = actorSystem.executionContext

  private def masterSequencerConnection(obsMode: String) = HttpConnection(ComponentId(Prefix(ESW, obsMode), Sequencer))

  def resolveMasterSequencerOf(observingMode: String): Future[Either[EswLocationError, HttpLocation]] =
    locationServiceUtil.resolve(masterSequencerConnection(observingMode), Timeouts.DefaultTimeout)

  def startSequencers(observingMode: String, requiredSequencers: Sequencers, retryCount: Int): Future[ConfigureResponse] =
    async {
      def masterSequencerId = ComponentId(Prefix(ESW, observingMode), Sequencer)

      val spawnSequencerResponses =
        await(FutureUtils.sequential(requiredSequencers.subsystems)(startSequencer(_, observingMode, retryCount))).sequence

      spawnSequencerResponses match {
        case Left(failedScriptResponses) => FailedToStartSequencers(failedScriptResponses.map(_.msg).toSet)
        case Right(_)                    => Success(masterSequencerId)
      }
    }

  def stopSequencers(sequencers: Sequencers, obsMode: String): Future[Either[RegistrationListingFailed, Done]] =
    Future
      .traverse(sequencers.subsystems) { subsystem =>
        resolveSequencer(obsMode, subsystem)
          .flatMap {
            case Left(listingFailed: RegistrationListingFailed) => throw listingFailed
            case Left(LocationNotFound(_))                      => Future.successful(Done)
            case Right(sequencerApi)                            => stopSequencer(sequencerApi)
          }
      }
      .map(_ => Right(Done))
      .recover { case listingFailed: RegistrationListingFailed => Left(listingFailed) }

  // spawn the sequencer on available SequenceComponent
  def startSequencer(
      subSystem: Subsystem,
      observingMode: String,
      retryCount: Int
  ): Future[Either[SequencerError, AkkaLocation]] =
    sequenceComponentUtil
      .getAvailableSequenceComponent(subSystem)
      .flatMap {
        case Right(seqCompApi)         => loadScript(subSystem, observingMode, seqCompApi)
        case Left(_) if retryCount > 0 => startSequencer(subSystem, observingMode, retryCount - 1)
        case Left(e)                   => Future.successful(Left(e))
      }

  // get sequence component from Sequencer and unload sequencer script
  private def stopSequencer(api: SequencerApi) = api.getSequenceComponent.flatMap(sequenceComponentUtil.unloadScript)

  // Created in order to mock the behavior of sequencer API availability for unit test
  private[sm] def createSequencerClient(location: Location): SequencerApi = SequencerApiFactory.make(location)
  private def resolveSequencer(obsMode: String, subsystem: Subsystem) =
    locationServiceUtil
      .resolveSequencer(subsystem, obsMode, Timeouts.DefaultTimeout)
      .mapRight(createSequencerClient)

  private def loadScript(subSystem: Subsystem, observingMode: String, seqCompApi: SequenceComponentApi) =
    seqCompApi
      .loadScript(subSystem, observingMode)
      .map(_.response.left.map(e => SequenceManagerError.LoadScriptError(e.msg)))

}
