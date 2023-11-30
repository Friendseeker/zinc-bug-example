
ThisBuild / scalaVersion     := "2.12.18"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "zinc-bug-example"
  )
libraryDependencies += "org.scala-sbt" %% "zinc" % "1.9.0-SNAPSHOT"
resolvers += Resolver.file("Local Ivy Repository", new File(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)

