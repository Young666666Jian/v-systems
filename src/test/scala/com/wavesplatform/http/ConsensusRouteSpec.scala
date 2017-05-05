package com.wavesplatform.http

import com.wavesplatform.BlockGen
import com.wavesplatform.history.HistoryWriterImpl
import com.wavesplatform.http.ApiMarshallers._
import com.wavesplatform.settings.FunctionalitySettings
import com.wavesplatform.state2.HistoryTest
import com.wavesplatform.state2.reader.StateReader
import org.h2.mvstore.MVStore
import org.scalamock.scalatest.MockFactory
import org.scalatest.DoNotDiscover
import org.scalatest.prop.PropertyChecks
import play.api.libs.json.JsObject
import scorex.api.http.BlockNotExists
import scorex.consensus.nxt.api.http.NxtConsensusApiRoute
import scorex.crypto.encode.Base58

@DoNotDiscover
class ConsensusRouteSpec extends RouteSpec("/consensus") with RestAPISettingsHelper with PropertyChecks with MockFactory with BlockGen with HistoryTest {
  private val state = mock[StateReader]

  private val history = new HistoryWriterImpl(new MVStore.Builder().open())
  appendGenesisBlock(history)
  for (i <- 1 to 10) appendTestBlock(history)

  private val route = NxtConsensusApiRoute(restAPISettings, state, history, FunctionalitySettings.TESTNET).route

  routePath("/generationsignature") - {
    "for last block" in {
      Get(routePath("/generationsignature")) ~> route ~> check {
        (responseAs[JsObject] \ "generationSignature").as[String] shouldEqual Base58.encode(history.lastBlock.consensusData.generationSignature)
      }
    }

    "for existed block" in {
      val block = history.blockAt(3).get
      Get(routePath(s"/generationsignature/${block.encodedId}")) ~> route ~> check {
        (responseAs[JsObject] \ "generationSignature").as[String] shouldEqual Base58.encode(block.consensusData.generationSignature)
      }
    }

    "for not existed block" in {
      Get(routePath(s"/generationsignature/brggwg4wg4g")) ~> route should produce(BlockNotExists)
    }
  }

  routePath("/basetarget") - {
    "for existed block" in {
      val block = history.blockAt(3).get
      Get(routePath(s"/basetarget/${block.encodedId}")) ~> route ~> check {
        (responseAs[JsObject] \ "baseTarget").as[Long] shouldEqual block.consensusDataField.value.baseTarget
      }
    }

    "for not existed block" in {
      Get(routePath(s"/basetarget/brggwg4wg4g")) ~> route should produce(BlockNotExists)
    }
  }
}
