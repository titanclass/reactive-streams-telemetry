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
        library.dropWizardMetricsCore,
        library.openTracingApi,
        library.utest % Test
      )
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val akka              = "2.5.21"
      val dropWizardMetrics = "4.0.5"
      val openTracing       = "0.31.0"
      val utest             = "0.6.6"
    }
    val akkaStream                        = "com.typesafe.akka"             %% "akka-stream"                            % Version.akka
    val dropWizardMetricsCore             = "io.dropwizard.metrics"         %  "metrics-core"                           % Version.dropWizardMetrics
    val openTracingApi                    = "io.opentracing"                %  "opentracing-api"                        % Version.openTracing
    val utest                             = "com.lihaoyi"                   %% "utest"                                  % Version.utest
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
  scalafmtSettings

lazy val commonSettings =
  Seq(
    scalaVersion := "2.12.8",
    organization := "au.com.titanclass",
    organizationName := "Titan Class Pty Ltd",
    startYear := Some(2019),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding", "UTF-8",
      "-Ypartial-unification",
      "-Ywarn-unused-import",
    ),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    wartremoverWarnings in (Compile, compile) ++= Warts.unsafe,
)

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true,
  )
