package com.wavesplatform.state2

import java.nio.charset.StandardCharsets

import cats._
import org.scalatest.{FunSuite, Matchers}

class PortfolioTest extends FunSuite with Matchers {
  test("pessimistic - should return only withdraws") {
    val Seq(fooKey, barKey, bazKey) = Seq("foo", "bar", "baz").map(x => ByteStr(x.getBytes(StandardCharsets.UTF_8)))

    val orig = Portfolio(
      balance = -10,
      leaseInfo = LeaseInfo(
        leaseIn = 11,
        leaseOut = 12
      ),
      assets = Map(
        fooKey -> -13,
        barKey -> 14,
        bazKey -> 0
      )
    )

    val p = orig.pessimistic
    p.balance shouldBe orig.balance
    p.leaseInfo.leaseIn shouldBe 0
    p.leaseInfo.leaseOut shouldBe orig.leaseInfo.leaseOut
    p.assets(fooKey) shouldBe orig.assets(fooKey)
    p.assets shouldNot contain(barKey)
    p.assets shouldNot contain(bazKey)
  }

  test("pessimistic - positive balance is turned into zero") {
    val orig = Portfolio(
      balance = 10,
      leaseInfo = LeaseInfo(0, 0),
      assets = Map.empty
    )

    val p = orig.pessimistic
    p.balance shouldBe 0
  }

  test("prevents overflow of assets") {
    val assetId = ByteStr(Array.empty)
    val arg1 = Portfolio(0L, LeaseInfo.empty, Map(assetId -> (Long.MaxValue - 1L)))
    val arg2 = Portfolio(0L, LeaseInfo.empty, Map(assetId -> (Long.MaxValue - 2L)))
    Monoid.combine(arg1, arg2).assets(assetId) shouldBe Long.MinValue
  }
}