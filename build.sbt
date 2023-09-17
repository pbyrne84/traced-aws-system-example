lazy val projectName = "traced-aws-system-example"

name := projectName
val scala213Version = "2.13.10"
scalaVersion := scala213Version

ThisBuild / turbo := false

ThisBuild / useSuperShell := false

//addCompilerPlugin("io.tryp" % "splain" % "0.5.8" cross CrossVersion.patch)

/*scalacOptions := List(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-explaintypes", // Explain type errors in more detail.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros", // Allow macro definition (besides implementation and application)
  "-language:higherKinds", // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
  "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
  "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
  "-Xlint:option-implicit", // Option.apply used implicit view.
  "-Xlint:package-object-classes", // Class or object defined in package object.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
  "-Ywarn-numeric-widen", // Warn when numerics are widened.
  "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals", // Warn if a local definition is unused.
  "-Ywarn-unused:params", // Warn if a value parameter is unused.
  "-Ywarn-unused:privates", // Warn if a private member is unused.
  "-Ycache-plugin-class-loader:last-modified", // Enables caching of classloaders for compiler plugins
  "-Ycache-macro-class-loader:last-modified", // and macro definitions. This can lead to performance improvements.
  "-Ywarn-macros:after",
  "-P:splain:implicits:true",
  "-P:splain:color:false"
)*/

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

val circeVersion = "0.14.3"
val akkaVersion = "2.8.0"
val akkaHttpVersion = "10.5.0"

//not to be used in ci, intellij has got a bit bumpy in the format on save on optimize imports across the project
val formatAndTest =
  taskKey[Unit](
    "format all code then run tests, do not use on CI as any changes will not be committed"
  )

lazy val commonSettings = Seq(
  scalaVersion := scala213Version,
  libraryDependencies ++= Vector(
    "ch.qos.logback.contrib" % "logback-json-classic" % "0.1.5",
    "ch.qos.logback.contrib" % "logback-jackson" % "0.1.5",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
    "com.chuusai" %% "shapeless" % "2.3.10",
//    "io.circe" %% "circe-parser" % circeVersion,
//    "io.circe" %% "circe-generic" % circeVersion,
//    "org.scalatest" %% "scalatest" % "3.2.9" % Test,
//    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion % Test,
//    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
//    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
//    "de.heikoseeberger" %% "akka-http-circe" % "1.39.2" % Test
  ),
  scalacOptions ++= Seq(
    "-encoding",
    "utf8",
    "-feature",
    "-language:implicitConversions",
    "-language:existentials",
    "-unchecked"
  ) ++
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) => Seq("-Ytasty-reader") // flags only needed in Scala 2
      case Some((3, _)) => Seq("-no-indent") // flags only needed in Scala 3
      case _ => Seq.empty
    }),
  formatAndTest := {
    (Test / test)
      .dependsOn(Compile / scalafmtAll)
      .dependsOn(Test / scalafmtAll)
  }.value,
  Test / test := (Test / test)
    .dependsOn(Compile / scalafmtCheck)
    .dependsOn(Test / scalafmtCheck)
    .value
)

Test / test := (Test / test)
  .dependsOn(Compile / scalafmtCheck)
  .dependsOn(Test / scalafmtCheck)
  .value

val scalaTest = "org.scalatest" %% "scalatest" % "3.2.13"

lazy val tracedPlay = (project in file("modules/scala/tracedPlay"))
  .settings(
    name := "tracedPlay",
    commonSettings,
    fork := true,
    javaAgents += "io.kamon" % "kanela-agent" % "1.0.17" % "runtime;compile",
    libraryDependencies ++= List(
      guice,
      "de.heikoseeberger" %% "akka-http-circe" % "1.39.2",
      "io.kamon" %% "kamon-akka-http" % "2.6.0",
      "io.kamon" %% "kamon-scala-future" % "2.6.0",
      "io.kamon" %% "kamon-play" % "2.6.0",
      "io.kamon" %% "kamon-zipkin" % "2.6.0",
      "io.kamon" %% "kamon-logback" % "2.6.1",
      "com.typesafe.play" %% "play" % "2.8.19",
      ws,
      scalaTest % Test
    )
  )
  .enablePlugins(PlayScala, JavaAgent)

lazy val tracedAkkaHttp = (project in file("modules/scala/tracedAkkaHttp"))
  .settings(
    name := "tracedAkkaHttp",
    commonSettings,
    fork := true,
    javaAgents += "io.kamon" % "kanela-agent" % "1.0.17" % "runtime;compile",
    libraryDependencies ++= List(
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "de.heikoseeberger" %% "akka-http-circe" % "1.39.2",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.15.1",
      "ch.qos.logback.contrib" % "logback-json-classic" % "0.1.5",
      "ch.qos.logback.contrib" % "logback-jackson" % "0.1.5",
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
      "io.kamon" %% "kamon-zipkin" % "2.6.0",
      "io.kamon" %% "kamon-logback" % "2.6.1",
      "io.kamon" %% "kamon-akka-http" % "2.6.0",
      "io.kamon" %% "kamon-scala-future" % "2.6.0",
      scalaTest % Test
    )
  )
  .enablePlugins(JavaAgent)

lazy val tracedZioHttp = (project in file("modules/scala/tracedZioHttp"))
  .settings(
    name := "tracedZioHttp",
    commonSettings,
    libraryDependencies ++= Vector(
      scalaTest % "provided"
    )
  )

lazy val allScala = (project in file("."))
  .aggregate(tracedPlay, tracedAkkaHttp, tracedZioHttp)
  .settings(
    commonSettings,
    publish / skip := true
  )

addCommandAlias("runplay", "tracedPlay/run")
addCommandAlias("runAkkaHttp", "tracedAkkaHttp/run")
