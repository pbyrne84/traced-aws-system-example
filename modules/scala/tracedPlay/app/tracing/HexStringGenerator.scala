package tracing

import java.security.SecureRandom
import javax.inject.Inject

class HexStringGenerator @Inject {

  private val random = new SecureRandom()

  def randomHex(bytes: Int): String = {
    val buffer = new Array[Byte](bytes)
    random.nextBytes(buffer)
    buffer.map(b => f"${b & 0xff}%02x").mkString
  }

}
