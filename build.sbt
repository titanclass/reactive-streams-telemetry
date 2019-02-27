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
        library.jaegerCore,
        library.sprayJson,
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
      val jaeger            = "0.27.0"
      val sprayJson         = "1.3.5"
      val utest             = "0.6.6"
    }
    val akkaStream                        = "com.typesafe.akka"             %% "akka-stream"                            % Version.akka
    val dropWizardMetricsCore             = "io.dropwizard.metrics"         %  "metrics-core"                           % Version.dropWizardMetrics
    val jaegerCore                        = "io.jaegertracing"              %  "jaeger-core"                            % Version.jaeger
    val sprayJson                         = "io.spray"                      %% "spray-json"                             % Version.sprayJson
    val utest                             = "com.lihaoyi"                   %% "utest"                                  % Version.utest
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++ scalafmtSettings

lazy val commonSettings =
  Seq(
    scalaVersion := "2.12.8",
    organization := "au.com.titanclass",
    organizationName := "Titan Class Pty Ltd",
    organizationHomepage := Some(url("https://www.titanclass.com.au")),
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
    useGpg := true,
    wartremoverWarnings in (Compile, compile) ++= Warts.unsafe,

    // Maven Central publishing
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/your-account/your-project"),
        "scm:git@github.com:your-account/your-project.git"
      )
    ),
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
