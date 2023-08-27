addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("com.timushev.sbt" % "sbt-rewarn" % "0.1.3")
addSbtPlugin(("com.typesafe.play" % "sbt-plugin" % "2.8.19").exclude("com.typesafe.sbt", "sbt-native-packager"))
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("com.typesafe.play" % "sbt-twirl" % "1.5.2")
addSbtPlugin(("io.kamon" % "sbt-kanela-runner-play-2.8" % "2.0.14").exclude("com.typesafe.sbt", "sbt-native-packager"))
