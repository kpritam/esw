package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.SubmitSequenceAndWait
import csw.event.api.scaladsl.EventSubscriber
import csw.event.client.EventServiceFactory
import csw.location.api.extensions.URIExtension.RichURI
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.StringKey
import csw.params.core.models.Prefix
import csw.params.events.EventKey
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.internal.Timeouts
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime

class LockUnlockIntegrationTest extends ScalaTestFrameworkTestKit(EventServer) with BaseTestSuite {
  import frameworkTestKit.frameworkWiring.actorRuntime._
  import frameworkTestKit.frameworkWiring.locationService

  private var ocsWiring: SequencerWiring           = _
  private var ocsSequencer: ActorRef[SequencerMsg] = _
  private var eventSubscriber: EventSubscriber     = _
  private implicit val askTimeout: Timeout         = Timeouts.DefaultTimeout

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  override def beforeAll(): Unit = {
    super.beforeAll()
    frameworkTestKit.spawnStandalone(ConfigFactory.load("standalone.conf"))
    eventSubscriber = new EventServiceFactory().make(locationService).defaultSubscriber
  }

  override def beforeEach(): Unit = {
    ocsWiring = new SequencerWiring("esw", "lockUnlockScript", None)
    ocsSequencer = ocsWiring.sequencerServer.start().rightValue.uri.toActorRef.unsafeUpcast[SequencerMsg]
  }

  override def afterEach(): Unit = {
    ocsWiring.sequencerServer.shutDown().futureValue
  }

  "Script" must {
    "support locking components | ESW-126" in {
      val probe = TestProbe[String]
      val key   = StringKey.make("lockResponse")
      eventSubscriber
        .subscribeCallback(Set(EventKey("esw.test.lock_response")), event => {
          val param = event.paramType.get(key).flatMap(_.get(0))
          param.foreach(probe.ref ! _)
        })

      val lockCommand = Setup(Prefix("TCS.test"), CommandName("lock-assembly"), None)
      ocsSequencer ! SubmitSequenceAndWait(Sequence(lockCommand), TestProbe[SubmitResponse].ref)

      probe.expectMessage("LockAcquired")
      probe.expectMessage("LockExpiringShortly")
      probe.expectMessage("LockExpired")
    }

    "support unlocking components | ESW-126" in {
      val eventSubscriber = new EventServiceFactory().make(locationService).defaultSubscriber
      val unlockCommand   = Setup(Prefix("TCS.test"), CommandName("unlock-assembly"), None)
      val unlockKey       = StringKey.make("unlockResponse")

      ocsSequencer ! SubmitSequenceAndWait(Sequence(unlockCommand), TestProbe[SubmitResponse].ref)
      eventually {
        val unlockEvent = eventSubscriber.get(EventKey("esw.test.unlock_response")).futureValue
        unlockEvent.paramType.get(unlockKey).flatMap(_.get(0)) should ===(Some("LockAlreadyReleased"))
      }
    }
  }
}