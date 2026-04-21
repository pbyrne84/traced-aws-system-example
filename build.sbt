lazy val projectName = "traced-aws-system-example"

name := projectName
val scala213Version = "3.8.3"
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

val circeVersion = "0.14.15"
val pekkoHttpVersion = "1.3.0"

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
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.6"
  ),
  scalacOptions ++= Seq(
    "-encoding",
    "utf8",
    "-feature",
    "-language:implicitConversions",
    "-language:existentials",
    "-unchecked",
    "-no-indent"
  ) ++
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 13)) => Seq("-Ytasty-reader") // flags only needed in Scala 2
      case Some((3, _))  => Seq("-no-indent") // flags only needed in Scala 3
      case _             => Seq.empty
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

val scalaTest = "org.scalatest" %% "scalatest" % "3.2.20"

lazy val tracedPlay = (project in file("modules/scala/tracedPlay"))
  .settings(
    name := "tracedPlay",
    commonSettings,
    fork := true,
    // javaAgents += "io.kamon" % "kanela-agent" % "2.0.0" % "runtime;compile",
    libraryDependencies ++= List(
      guice,
      "io.kamon" %% "kamon-pekko-http" % "2.8.1",
      "io.kamon" %% "kamon-scala-future" % "2.8.1",
      "io.circe" %% "circe-parser" % "0.14.15",
      "io.kamon" %% "kamon-zipkin" % "2.8.1",
      "io.kamon" %% "kamon-logback" % "2.8.1",
      "org.playframework" %% "play" % "3.0.10",
      ws,
      scalaTest % Test
    )
  )
  .enablePlugins(PlayScala)

//"org.apache.pekko" %% "pekko-http" % "1.3.0"
lazy val tracedAkkaHttp = (project in file("modules/scala/tracedAkkaHttp"))
  .settings(
    name := "tracedAkkaHttp",
    commonSettings,
    fork := true,
    javaAgents += "io.kamon" % "kanela-agent" % "2.0.0" % "runtime;compile",
    libraryDependencies ++= List(
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "org.mdedetrich" %% "pekko-http-circe" % "1.1.0",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.21.2",
      "ch.qos.logback.contrib" % "logback-json-classic" % "0.1.5",
      "ch.qos.logback.contrib" % "logback-jackson" % "0.1.5",
      "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-stream-testkit" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-testkit" % pekkoHttpVersion,
      "io.kamon" %% "kamon-bundle" % "2.8.1",
      // "io.kamon" %% "kamon-apm-reporter" % "2.8.1",
      "io.kamon" %% "kamon-zipkin" % "2.8.1",
      "io.kamon" %% "kamon-logback" % "2.8.1",
      "io.kamon" %% "kamon-pekko-http" % "2.8.1",
      "io.kamon" %% "kamon-scala-future" % "2.8.1",
      "net.logstash.logback" % "logstash-logback-encoder" % "9.0",
      "ch.qos.logback" % "logback-classic" % "1.5.32",
      scalaTest % Test
    )
  )
  .enablePlugins(JavaAgent)

val openTelemetryVersion = "1.61.0"
val zioLoggingVersion = "2.5.3"

lazy val tracedZioHttp = (project in file("modules/scala/tracedZioHttp"))
  .settings(
    name := "tracedZioHttp",
    mainClass := Some("com.github.pbyrne84.ziozipkin.ZIOHttpZipkinApp"),
    commonSettings,
    libraryDependencies ++= Vector(
      "ch.qos.logback" % "logback-classic" % "1.5.32",
      "dev.zio" %% "zio" % "2.1.25",
      "dev.zio" %% "zio-http" % "3.10.1",
      "io.opentracing" % "opentracing-util" % "0.33.0",
      "dev.zio" %% "zio-opentelemetry" % "3.1.15",
      "dev.zio" %% "zio-opentracing" % "3.1.15",
      "io.jaegertracing" % "jaeger-core" % "1.8.1",
      "io.jaegertracing" % "jaeger-client" % "1.8.1",
      "io.jaegertracing" % "jaeger-zipkin" % "1.8.1",
      "org.slf4j" % "jul-to-slf4j" % "2.0.17",
      "net.logstash.logback" % "logstash-logback-encoder" % "9.0",
      "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion,
      "dev.zio" %% "zio-logging-slf4j-bridge" % zioLoggingVersion,
      "io.opentelemetry" % "opentelemetry-extension-trace-propagators" % openTelemetryVersion,
      "io.opentelemetry" % "opentelemetry-exporter-zipkin" % openTelemetryVersion,
      "dev.zio" %% "zio-test" % "2.1.25" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.1.25" % Test,
      "dev.zio" %% "zio-http-testkit" % "3.10.1" % Test
    )
  )

lazy val runAll = (project in file("modules/scala/runAll"))
  .settings(
    name := "tracedZioHttp",
    commonSettings
  )

lazy val allScala = (project in file("."))
  .aggregate(tracedPlay, tracedAkkaHttp, tracedZioHttp)
  .settings(
    commonSettings,
    publish / skip := true
  )

addCommandAlias("runplay", "tracedPlay/run")
addCommandAlias("runAkkaHttp", "tracedAkkaHttp/run")
addCommandAlias("runZio", "tracedZioHttp/run")
