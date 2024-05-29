ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3" //pekko website

val PekkoVersion = "1.0.2"
val scalaTestVersion =  "3.0.5"  //"3.2.18"   //"3.0.5"

lazy val root = (project in file("."))
  .settings(
    name := "pekko-by-example",
    libraryDependencies ++=  Seq(
      "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
      "org.apache.pekko" %% "pekko-stream-testkit" % PekkoVersion, //% Test
      //"org.scalatest" %% "scalatest" % scalaTestVersion
      //"org.scalactic" %%% "scalactic" % "3.2.18",
//      "org.scalatest" %%% "scalatest" % scalaTestVersion
    )
  )
