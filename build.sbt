name := "rbackup-scala-client-updater"
version := sys.env.getOrElse("VERSION", "0.1.0")

scalaVersion := "2.12.7"

mainClass in (Compile, run) := Some("cz.jenda.rbackup.updater.Main")
mainClass in assembly := Some("cz.jenda.rbackup.updater.Main")


libraryDependencies ++= Seq(
  "com.softwaremill.sttp" %% "core" % "1.5.1",
  "org.apache.commons" % "commons-lang3" % "3.8.1",
  "com.github.pathikrit" %% "better-files" % "3.6.0",
  "org.typelevel" %% "cats-core" % "1.5.0",
  "org.scalatest" %% "scalatest" % "3.0.5",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "io.sentry" % "sentry-logback" % "1.7.14"
)

lazy val setVersionInSources = taskKey[Unit]("Sets Sentry DSN into sources")

setVersionInSources := {
  import java.io.PrintWriter

  import scala.io.Source

  val path = "src/main/scala/cz/jenda/rbackup/updater/Main.scala"

  sys.env.get("VERSION").foreach { version =>
    println(s"Setting version to $version")

    val src = Source.fromFile(path).mkString
    val updated = src.replace(
      """SentryDsn: Option[String] = None""",
      s"""SentryDsn: Option[String] = Some("$version")"""
    )

    val writer = new PrintWriter(new File(path))
    writer.write(updated)
    writer.close()
  }
}

lazy val setSentryDsnInSources = taskKey[Unit]("Sets Sentry DSN into sources")

setSentryDsnInSources := {
  import java.io.PrintWriter

  import scala.io.Source

  val path = "src/main/scala/cz/jenda/rbackup/updater/Main.scala"

  sys.env.get("SENTRY_DSN").foreach { dsn =>
    println(s"Setting Sentry DSN")

    val src = Source.fromFile(path).mkString
    val updated = src.replace(
      """SentryDsn: Option[String] = None""",
      s"""SentryDsn: Option[String] = Some("$dsn")"""
    )

    val writer = new PrintWriter(new File(path))
    writer.write(updated)
    writer.close()
  }
}

assemblyMergeStrategy in assembly := {
  case manifest if manifest.contains("MANIFEST.MF") =>
    MergeStrategy.discard
  case manifest if manifest.contains("META-INF") =>
    MergeStrategy.discard
  case reference if reference.contains("reference.conf") =>
    MergeStrategy.concat
  case _ => MergeStrategy.deduplicate
}