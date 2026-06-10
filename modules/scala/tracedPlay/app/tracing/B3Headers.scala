package tracing

object B3Headers {

  private val hexStringGenerator = new HexStringGenerator()

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

  private def createUnsafe[A](attempt: String => Either[String, A], length: Int): A = {
    attempt(hexStringGenerator.randomHex(length)) match {
      case Left(value) =>
        val secondAttemptValue = hexStringGenerator.randomHex(length)
        attempt(secondAttemptValue) match {
          // this block should never be called as we should never fail, especially twice, and would be indicative of a
          // larger problem
          case Left(value)               => throw new InvalidIdGenerationException(length, secondAttemptValue)
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
