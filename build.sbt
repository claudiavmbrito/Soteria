name := "SOTERIA"

version := "1.0.0"

scalaVersion := "2.12.15"

val sparkVersion = "3.2.0"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  
  // Logging
  "org.slf4j" % "slf4j-api" % "1.7.36",
  "org.slf4j" % "slf4j-log4j12" % "1.7.36",
  
  // Testing
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test,
  
  // JSON processing
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.13.3",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.3"
)

// Assembly plugin settings for creating fat JARs
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "application.conf" => MergeStrategy.concat
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}

// Compiler options
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-Xlint"
)

// Java compatibility
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

// Test settings
Test / parallelExecution := false
