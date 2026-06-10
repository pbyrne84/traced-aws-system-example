package tracing

import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers.*
import tracing.B3Headers.TraceId

class B3HeadersSpec extends AnyFreeSpecLike {

  "B3Headers" - {

    "TraceId attempt" - {

      "should pass on valid non zero string of 16 characters" in {
        val valid16CharTraceIdHex = "abcdef0123456abc"
        TraceId.attempt(valid16CharTraceIdHex) shouldBe Right(TraceId(valid16CharTraceIdHex))
      }

      "should fail on a 32 character non hexadecimal string" in {
        val invalid16CharNonHexTraceId = "gbcdef0123456abc"
        TraceId.attempt(invalid16CharNonHexTraceId) shouldBe Left(
          createFailureMessage(invalid16CharNonHexTraceId)
        )
      }

      "should fail on a hex string larger than 16 characters" in {
        val invalid17CharTraceIdHex = "abcddefabcdef012a"

        TraceId.attempt(invalid17CharTraceIdHex) shouldBe Left(
          createFailureMessage(invalid17CharTraceIdHex)
        )
      }

      def createFailureMessage(value: String): String =
        s"id regex ^[a-f|\\d]{32}$$ did not match '$value'"

      "should fail on a hex string shorter than 16 characters" in {
        val invalid15CharTraceIdHex = "abcdeef0123456a"

        TraceId.attempt(invalid15CharTraceIdHex) shouldBe Left(
          createFailureMessage(invalid15CharTraceIdHex)
        )
      }

      "should fail on an empty string" in {
        TraceId.attempt("") shouldBe Left(
          createFailureMessage("")
        )
      }

      "should fail on a string that is all zeros" in {
        val allZero32CharTraceIdHex = "0000000000000000"

        TraceId.attempt(allZero32CharTraceIdHex) shouldBe Left(
          "id needs to be a non 0 filled hexadecimal string of 16 characters"
        )
      }

    }
  }
}
