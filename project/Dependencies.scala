import sbt._

object Dependencies {

  lazy val circeCoreVersion = "0.14.5"
  lazy val compile          = Seq(
    "com.github.fd4s"  %% "fs2-kafka"     % "3.0.1",
    "org.typelevel"    %% "cats-effect"   % "3.4.8",
    "com.github.cb372" %% "cats-retry"    % "3.1.0",
    "io.circe"         %% "circe-core"    % "0.14.5",
    "io.circe"         %% "circe-parser"  % circeCoreVersion,
    "io.circe"         %% "circe-generic" % circeCoreVersion,
  )

  lazy val test = Seq("org.scalatest" %% "scalatest" % "3.2.15").map(_ % Test)
}
