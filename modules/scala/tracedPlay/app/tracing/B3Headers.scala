package tracing

import java.math.BigInteger
import java.security.SecureRandom

object B3Headers {

  private val hexStringGenerator = new HexStringGenerator()
  private val secureRandom = new SecureRandom()

  object name {
    val traceId: String = "X-B3-TraceId"
    val spanId: String = "X-B3-SpanId"
  }

  class InvalidIdGenerationException(length: Int, value: String)
      extends RuntimeException(s"failed generating valid hex id of $length chars after 2 attempts, last value: $value")

  object TraceId {

    private val length = 16

    def createUnsafe: TraceId =
      B3Headers.createUnsafe(attempt, 16)

    def attempt(value: String): Either[String, TraceId] =
      attemptCreatingId(TraceId.apply, value, length)
  }

  case class TraceId private[tracing] (value: String)

  private def createUnsafe[A](attempt: String => Either[String, A], maxLength: Int): A = {
    def generateRandomHex: String = {
      val bigInteger: BigInteger = new BigInteger(maxLength * 4, secureRandom)
      val hexValue: String = bigInteger.toString(16)

      val length = hexValue.length
      if (length < maxLength) {
        "0".padTo(maxLength - hexValue.length, "0").mkString + hexValue
      } else {
        hexValue
      }
    }

    val paddedHexValue: String = generateRandomHex
    attempt(paddedHexValue) match {
      case Left(value) =>
        val secondAttemptValue = generateRandomHex
        attempt(secondAttemptValue) match {
          // this block should never be called as we should never fail, especially twice, and would be indicative of a
          // larger problem
          case Left(value)               => throw new InvalidIdGenerationException(16, secondAttemptValue)
          case Right(secondAttemptValue) => secondAttemptValue
        }
      case Right(id) => id
    }
  }

  private def attemptCreatingId[A](apply: String => A, value: String, length: Int): Either[String, A] = {
    val regex = s"^[a-f|\\d]{$length}$$"
    val allZeroString = "0".padTo(length, "0").mkString
    val lowerCaseValue = value.toLowerCase

    if (lowerCaseValue == allZeroString) {
      Left(s"id needs to be a non 0 filled hexadecimal string of $length characters")
    } else if (lowerCaseValue.matches(regex)) {
      Right(apply(lowerCaseValue))
    } else {
      Left(s"id regex $regex did not match '$lowerCaseValue'")
    }
  }

  object SpanId {

    def attempt(value: String): Either[String, SpanId] =
      attemptCreatingId(SpanId.apply, value, 8)

    def createUnsafe: SpanId = B3Headers.createUnsafe(attempt, 8)
  }

  case class SpanId private[tracing] (value: String)

}
