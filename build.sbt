ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.15"

// Spark
libraryDependencies += "org.apache.spark" %% "spark-core" % "3.2.1"
libraryDependencies += "org.apache.spark" %% "spark-streaming" % "3.2.1"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.2.1"

// Twitter4j
libraryDependencies += "org.twitter4j" % "twitter4j-core" % "4.0.7"
libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "4.0.7"
libraryDependencies += "org.apache.bahir" %% "spark-streaming-twitter" % "2.4.0"

// Mongo Spark
libraryDependencies += "org.mongodb.spark" %% "mongo-spark-connector" % "3.0.1"

// Kafka
libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.2.1"

// Json
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.2"

// HBC Java Client
libraryDependencies += "com.twitter" % "hbc-core" % "2.2.0"

// Mongo - Scala
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "4.5.1"

// Akka
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"   % "2.6.19",
  "com.typesafe.akka" %% "akka-slf4j"   % "2.6.19",
  "com.typesafe.akka" %% "akka-remote"  % "2.6.19",
  "com.typesafe.akka" %% "akka-agent"   % "2.5.32",
  "com.typesafe.akka" %% "akka-testkit" % "2.6.19" % "test",
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.19",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.19" % Test,
  "com.typesafe.akka" %% "akka-stream" % "2.6.19",
  "com.typesafe.akka" %% "akka-http" % "10.2.9"
)
lazy val root = (project in file("."))
  .settings(
    name := "covid-tweet-analysis"
  )
