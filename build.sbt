scalaVersion := "2.11.8"

name := "scorex-basics"
organization := "org.scorexfoundation"
version := "2.0.0-SNAPSHOT"

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

val circeVersion = "0.4.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.typesafe.akka" %% "akka-actor" % "2.+",
  "org.bitlet" % "weupnp" % "0.1.+",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.+",
  "com.chuusai" %% "shapeless" % "2.+",
  "io.swagger" %% "swagger-scala-module" % "1.+",
  "io.swagger" % "swagger-core" % "1.+",
  "io.swagger" % "swagger-annotations" % "1.+",
  "io.swagger" % "swagger-models" % "1.+",
  "io.swagger" % "swagger-jaxrs" % "1.+",
  "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.+",
  "ch.qos.logback" % "logback-classic" % "1.+",
  "ch.qos.logback" % "logback-core" % "1.+",
  "org.consensusresearch" %% "scrypto" % "1.2.0-RC2",
  "commons-net" % "commons-net" % "3.+",
  "com.typesafe.akka" %% "akka-testkit" % "2.+" % "test",
  "org.scalatest" %% "scalatest" % "2.+" % "test",
  "org.scalactic" %% "scalactic" % "2.+" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.+" % "test",
  "net.databinder.dispatch" %% "dispatch-core" % "+" % "test"
)

scalacOptions ++= Seq("-feature", "-deprecation")

javaOptions ++= Seq(
  "-server"
)

testOptions in Test += Tests.Argument("-oD", "-u", "target/test-reports")

//publishing settings

publishMavenStyle := true

publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

fork := true

pomIncludeRepository := { _ => false }

licenses := Seq("CC0" -> url("https://creativecommons.org/publicdomain/zero/1.0/legalcode"))

homepage := Some(url("https://github.com/ScorexFoundation/Scorex"))

pomExtra := (
  <scm>
    <url>git@github.com:ScorexFoundation/Scorex.git</url>
    <connection>scm:git:git@github.com:ScorexFoundation/Scorex.git</connection>
  </scm>
    <developers>
      <developer>
        <id>kushti</id>
        <name>Alexander Chepurnoy</name>
        <url>http://chepurnoy.org/</url>
      </developer>
    </developers>)

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
