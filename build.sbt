// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `reactive-streams-telemetry` =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.akkaStream,
        library.openTelemetryProto,
        library.openTelemetrySdk,
        library.utest % Test,
        library.akkaStreamTestkit % Test
      )
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val akka               = "2.6.9"
      val openTelemetryProto = "0.3.0"
      val openTelemetrySdk   = "0.8.0"
      val utest              = "0.7.5"
    }
    val akkaStream         = "com.typesafe.akka" %% "akka-stream"         % Version.akka
    val akkaStreamTestkit  = "com.typesafe.akka" %% "akka-stream-testkit" % Version.akka
    val openTelemetryProto = "io.opentelemetry"  %  "opentelemetry-proto" % Version.openTelemetryProto
    val openTelemetrySdk   = "io.opentelemetry"  %  "opentelemetry-sdk"   % Version.openTelemetrySdk
    val utest              = "com.lihaoyi"       %% "utest"               % Version.utest
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++ scalafmtSettings

lazy val commonSettings =
  Seq(
    scalaVersion := "2.13.3",
    organization := "au.com.titanclass",
    organizationName := "Titan Class Pty Ltd",
    organizationHomepage := Some(url("https://www.titanclass.com.au")),
    startYear := Some(2019),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    homepage := Some(url("https://github.com/titanclass/reactive-streams-telemetry")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/titanclass/reactive-streams-telemetry"),
        "scm:git@github.com:titanclass/reactive-streams-telemetry.git"
      )
    ),
    developers := List(
      Developer(
        id    = "huntc",
        name  = "Christopher Hunt",
        email = "huntchr@gmail.com",
        url   = url("http://christopherhunt-software.blogspot.com/")
      )
    ),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding", "UTF-8"
    ),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    wartremoverWarnings in (Compile, compile) ++= Warts.unsafe,

    // Maven Central publishing
    credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credential"),
    pomIncludeRepository := { _ => false },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true
  )

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true,
  )
