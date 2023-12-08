package com.github.pbyrne84.runall

import java.io.{BufferedReader, InputStreamReader}

object RunAllServices {

  def main(args: Array[String]): Unit = {

    val allProcesses: List[(String, Process)] = new RunAllServices().run

    println(allProcesses.size)

    allProcesses.tail.foreach {
      case (_, process) =>
        println("moo")
        try {
          val input = new BufferedReader(new InputStreamReader(process.getInputStream))
          try {
            var line: String = null
            while ((line = input.readLine) != null) System.out.println(line)
          } finally if (input != null) input.close()
        }
    }

    scala.sys.addShutdownHook(
      allProcesses.foreach {
        case (descriptor, process) =>
          println(s"Stopping '$descriptor' pid ${process.pid()}")
          process.destroyForcibly()
      }
    )

    while (Console.in.read != 22) {
      println("shutting down")
      System.exit(0)
    }
  }
}

class RunAllServices {

  def run: List[(String, Process)] = {

    println("starting akka http 8080")
    val httpProcess = createProcess("./runAkkaHttp.bat")

    println("starting play 9000")
    val play = createProcess("./runPlay.sh")

    List("akka http" -> httpProcess, "play" -> play)
  }

  private def createProcess[A](call: String): Process = {
    Runtime.getRuntime.exec(call)

  }
}
