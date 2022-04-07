ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.15"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.2.1"
libraryDependencies += "org.apache.spark" %% "spark-streaming" % "3.2.1"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.2.1"

libraryDependencies += "org.twitter4j" % "twitter4j-core" % "4.0.7"
libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "4.0.7"
libraryDependencies += "org.apache.bahir" %% "spark-streaming-twitter" % "2.4.0"

libraryDependencies += "org.mongodb.spark" %% "mongo-spark-connector" % "3.0.1"


libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.2.1"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.2"

libraryDependencies += "com.twitter" % "hbc-core" % "2.2.0"


// API Service
//libraryDependencies += "com.typesafe.akka" % "akka-actor-typed_2.13" % "2.6.19"
//libraryDependencies += "com.typesafe.akka" % "akka-stream-typed_2.13" % "2.6.19"
//libraryDependencies += "com.typesafe.akka" % "akka-http_2.13" % "10.2.9"
//libraryDependencies += "com.typesafe.akka" % "akka-http-spray-json_2.13" % "10.2.9"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"   % "2.6.19",
  "com.typesafe.akka" %% "akka-slf4j"   % "2.6.19",
  "com.typesafe.akka" %% "akka-remote"  % "2.6.19",
  "com.typesafe.akka" %% "akka-agent"   % "2.5.32",
  "com.typesafe.akka" %% "akka-testkit" % "2.6.19" % "test"
)

lazy val root = (project in file("."))
  .settings(
    name := "covid-tweet-analysis"
  )
