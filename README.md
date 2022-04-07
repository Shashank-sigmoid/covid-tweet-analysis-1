# Steps to follow

1. Create New Project in IntelliJ with following versions
```abdi
sbt version = 1.6.2
SDK = 11 (Amazon Corretto)
scala version = 2.12.15
Spark Version = 3.2.1
Confluent Version = 7.0.1
```

2. Add Following script to `build.sbt`
```sbt
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.15"

// Streaming 
libraryDependencies += "org.apache.spark" %% "spark-core" % "3.2.1"
libraryDependencies += "org.apache.spark" %% "spark-streaming" % "3.2.1"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.2.1"
// For Twitter 
libraryDependencies += "org.twitter4j" % "twitter4j-core" % "4.0.7"
libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "4.0.7"
libraryDependencies += "org.apache.bahir" %% "spark-streaming-twitter" % "2.4.0"
// Mongo Sink
libraryDependencies += "org.mongodb.spark" %% "mongo-spark-connector" % "3.0.1"

// Kafka Source
libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.2.1"


lazy val root = (project in file("."))
  .settings(
    name := "covid-tweet-analysis"
  )
```

3. Start kafka and zookeeper with the following command:
### CONFLUENT 

**Installation:**
- Because kafka works only on 1.8 (really?), we need to give `JAVA_HOME` path to 1.8 version + PATH of `CONFLUENT_HOME` to confluent we just installed at location `/Users/shantanu/confluent-7.0.1`
```bash
# List out the java versions and their path
/usr/libexec/java_home -V
# > Output
Matching Java Virtual Machines (3):
    17.0.1 (arm64) "Oracle Corporation" - "Java SE 17.0.1" /Library/Java/JavaVirtualMachines/jdk-17.0.1.jdk/Contents/Home
    16.0.2 (x86_64) "Amazon.com Inc." - "Amazon Corretto 16" /Users/shantanu/Library/Java/JavaVirtualMachines/corretto-16.0.2/Contents/Home
    1.8.0_322 (x86_64) "Amazon" - "Amazon Corretto 8" /Users/shantanu/Library/Java/JavaVirtualMachines/corretto-1.8.0_322/Contents/Home

vi .zshrc
# NOW ADD BELOW 3 export into .zshrc
# Export Amazon corretto 8 which is 1.8 version of java 
export JAVA_HOME=/Users/shantanu/Library/Java/JavaVirtualMachines/corretto-1.8.0_322/Contents/Home
# For confluent to work (find stable solution)
# Set HOME For confluent
export CONFLUENT_HOME=/Users/shantanu/confluent-7.0.1
# Add to path (temporary ig)
export PATH=$PATH:$CONFLUENT_HOME/bin
```
**Setup:**
```bash
# Run confluent services
confluent local services start
```
Now go to localhost:9021 and create a topic `test-topic` with default settings
```bash
kafka-console-producer --broker-list localhost:9092 --topic test-topic
>{"name": "Shantanu", "age": 22, "gender": "Male"}
>{"name": "Bhavesh", "age":21, "gender": "Male"}
```
Open another terminal for consumer
```bash
kafka-console-consumer --bootstrap-server localhost:9092 --topic test-topic --from-beginning
>{"name": "Shantanu", "age": 22, "gender": "Male"}
>{"name": "Bhavesh", "age":21, "gender": "Male"}
```

```bash
# Stop service
confluent local services stop
# Delete the metadata
confluent local destroy
```
### BREW
```bash
# Prerequisite for kafka
brew install java
# Install kafka (upto 5 min)
brew install kafka
# List services
brew services list
# May require to uncomment last 2 line as showed below
vi /opt/homebrew/etc/kafka/server.properties
```

- Change the following in the file `/opt/homebrew/etc/kafka/server.properties`

```bash
# The address the socket server listens on. It will get the value returned from
# java.net.InetAddress.getCanonicalHostName() if not configured.
#   FORMAT:
#     listeners = listener_name://host_name:port
#   EXAMPLE:
#  listeners =PLAINTEXT://your.host.name:9092
listeners=PLAINTEXT://:9092
advertised.listeners=PLAINTEXT://localhost:9092
```
```bash
# Start the services
brew services start zookeeper
brew services start kafka
# Create kafka topic
kafka-topics --create --topic test-topic --bootstrap-server localhost:9092 --replication-factor 1 --partitions 4
# Create producer console
kafka-console-producer --broker-list localhost:9092 --topic test-topic
> send first message
> send second message
> send third message
# Create consumer console in another terminal
kafka-console-consumer --bootstrap-server localhost:9092 --topic test-topic --from-beginning
send first message
send second message
send third message
```

4. Run file `TwitterToKafka.scala`. It will send data to Kafka from twitter using API hbc.
5. Run file `KafkaToMongo.scala`. It will extract useful data from Kafka and send useful data to MongoDB.
