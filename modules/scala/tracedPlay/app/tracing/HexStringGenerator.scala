package tracing

import java.security.SecureRandom
import javax.inject.Inject

class HexStringGenerator @Inject {

  private val random = new SecureRandom()

  def randomHex(bytes: Int): String = {

    println("woof" + bytes)
    val buffer = new Array[Byte](bytes)
    random.nextBytes(buffer)

    println(s"banaga ${buffer.toList.mkString}")

    val string = buffer.map(b => f"${b & 0xff}%02x").mkString

    println(s"meow $string")
    println(s"meow ${buffer.length}")
    string
  }

}
