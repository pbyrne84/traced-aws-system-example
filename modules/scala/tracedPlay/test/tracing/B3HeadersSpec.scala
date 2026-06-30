package tracing

import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers.*
import tracing.B3Headers.{SpanId, TraceId}

class B3HeadersSpec extends AnyFreeSpecLike {

  "B3Headers" - {

    "TraceId attempt" - {

      "should pass on valid non zero string of 16 characters" in {
        val valid16CharTraceIdHex = "abcdef0123456abc"
        TraceId.attempt(valid16CharTraceIdHex) shouldBe Right(TraceId(valid16CharTraceIdHex))
      }

      "should fail on a 16 character non hexadecimal string" in {
        val invalid16CharNonHexTraceId = "gbcdef0123456abc"
        TraceId.attempt(invalid16CharNonHexTraceId) shouldBe Left(
          createTraceIdFailureMessage(invalid16CharNonHexTraceId)
        )
      }

      def createTraceIdFailureMessage(value: String): String =
        s"id regex ^[a-f|\\d]{16}$$ did not match '$value'"

      "should fail on a hex string larger than 16 characters" in {
        val invalid17CharTraceIdHex = "abcddefabcdef012a"

        TraceId.attempt(invalid17CharTraceIdHex) shouldBe Left(
          createTraceIdFailureMessage(invalid17CharTraceIdHex)
        )
      }

      "should fail on a hex string shorter than 16 characters" in {
        val invalid15CharTraceIdHex = "abcdeef0123456a"

        TraceId.attempt(invalid15CharTraceIdHex) shouldBe Left(
          createTraceIdFailureMessage(invalid15CharTraceIdHex)
        )
      }

      "should fail on an empty string" in {
        TraceId.attempt("") shouldBe Left(
          createTraceIdFailureMessage("")
        )
      }

      "should fail on a string that is all zeros" in {
        val allZero16CharTraceIdHex = "0000000000000000"

        TraceId.attempt(allZero16CharTraceIdHex) shouldBe Left(
          "id needs to be a non 0 filled hexadecimal string of 16 characters"
        )
      }
    }

    "SpanId attempt" - {
      "should pass on valid non zero string of 8 characters" in {
        val valid8CharTraceIdHex = "abcdef01"
        SpanId.attempt(valid8CharTraceIdHex) shouldBe Right(SpanId(valid8CharTraceIdHex))
      }

      "should fail on a 16 character non hexadecimal string" in {
        val invalid8CharNonHexTraceId = "gbcdef012"
        SpanId.attempt(invalid8CharNonHexTraceId) shouldBe Left(
          createSpanIdFailureMessage(invalid8CharNonHexTraceId)
        )
      }

      def createSpanIdFailureMessage(value: String): String =
        s"id regex ^[a-f|\\d]{8}$$ did not match '$value'"

      "should fail on a hex string larger than 16 characters" in {
        val invalid9CharTraceIdHex = "abcddefabcdef012a"

        SpanId.attempt(invalid9CharTraceIdHex) shouldBe Left(
          createSpanIdFailureMessage(invalid9CharTraceIdHex)
        )
      }

      "should fail on an empty string" in {
        SpanId.attempt("") shouldBe Left(
          createSpanIdFailureMessage("")
        )
      }

      "should fail on a string that is all zeros" in {
        val allZero8CharTraceIdHex = "00000000"

        SpanId.attempt(allZero8CharTraceIdHex) shouldBe Left(
          "id needs to be a non 0 filled hexadecimal string of 8 characters"
        )
      }
    }

  }
}
