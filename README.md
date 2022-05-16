# Covid Tweet Analysis


# Problem Statement

[Link](https://docs.google.com/document/d/1u7evaLRQ_CFOKFJSi0JniaaI_4hoxFYAyMqHrtSwFuY/edit)

# Architecture

![structure](./images/structure.png)

# Steps

## Step 1 - Environment Setup


1. Create New Project in IntelliJ with following versions
```python
sbt version = 1.6.2
JDK = 11 (Amazon Corretto) (java 8 to remove warnings)
scala version = 2.12.15
Spark Version = 3.2.1
Confluent Version = 7.0.1
```

2. Use `build.sbt` to build the files
```scala
// ~~~~~~~~~
// ~~~~~~~~~
```

3. Start kafka and zookeeper with the following command:
### CONFLUENT 

**Installation:**
- Because kafka works only on 1.8 (true), we need to give `JAVA_HOME` path to 1.8 version + PATH of `CONFLUENT_HOME` to confluent we just installed at location `/Users/shantanu/confluent-7.0.1`
```bash
# List out the java versions and their path
~ /usr/libexec/java_home -V

# > Output
Matching Java Virtual Machines (3):
    17.0.1 (arm64) "Oracle Corporation" - "Java SE 17.0.1" /Library/Java/JavaVirtualMachines/jdk-17.0.1.jdk/Contents/Home
    16.0.2 (x86_64) "Amazon.com Inc." - "Amazon Corretto 16" /Users/shashank/Library/Java/JavaVirtualMachines/corretto-16.0.2/Contents/Home
    1.8.0_322 (x86_64) "Amazon" - "Amazon Corretto 8" /Users/shashank/Library/Java/JavaVirtualMachines/corretto-1.8.0_322/Contents/Home

~ vi .zshrc
# NOW ADD BELOW 3 export into .zshrc

# Export Amazon corretto 8 which is 1.8 version of java 
~ export JAVA_HOME=/Users/shashank/Library/Java/JavaVirtualMachines/corretto-1.8.0_322/Contents/Home

# For confluent to work (find stable solution)

# Set HOME For confluent
~ export CONFLUENT_HOME=/Users/shashank/confluent-7.0.1

# Add to path (temporary ig)
~ export PATH=$PATH:$CONFLUENT_HOME/bin
```
**Setup:**
```bash
# Run confluent services
~ confluent local services start
```
Now go to localhost:9021 and create a topic `test-topic` with default settings
```bash
~ kafka-console-producer --broker-list localhost:9092 --topic covid-tweet
>{"name": "Shantanu", "age": 22, "gender": "Male"}
>{"name": "Bhavesh", "age":21, "gender": "Male"}
```
Open another terminal for consumer
```bash
~ kafka-console-consumer --bootstrap-server localhost:9092 --topic covid-tweet --from-beginning
>{"name": "Shantanu", "age": 22, "gender": "Male"}
>{"name": "Bhavesh", "age":21, "gender": "Male"}
```

```bash
# Stop service
~ confluent local services stop

# Delete the metadata
~ confluent local destroy
```
### BREW
```bash
# Prerequisite for kafka
~ brew install java

# Install kafka (upto 5 min)
~ brew install kafka

# List services
~ brew services list

# May require to uncomment last 2 line as showed below
~ vi /opt/homebrew/etc/kafka/server.properties
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
```zsh
# Start the services
~ brew services start zookeeper
~ brew services start kafka

# Create kafka topic
kafka-topics --create --topic covid-tweet --bootstrap-server localhost:9092 --replication-factor 1 --partitions 4

# Create producer console
~ kafka-console-producer --broker-list localhost:9092 --topic covid-tweet
> send first message
> send second message
> send third message

# Create consumer console in another terminal
~ kafka-console-consumer --bootstrap-server localhost:9092 --topic covid-tweet --from-beginning
send first message
send second message
send third message
```

## STEP 2: Data Ingest from Twitter to Kafka (Using HBC client)

`TwitterToKafka.scala`
```scala
// ~~~~~~~~~~~
// ~~~~~~~~~~~
```

| **topic name** 	|                  **value**                 	|
|:--------------:	|:------------------------------------------:	|
| "key": "test-topic"     	| "value": { "$binary" : "eyJjcmVhdGVkX2F" } 	|
| "key": "test-topic"    	| "value": { "$binary" : "JadJjdafadDSFSF" } 	|


## STEP 3: Transform Data using Spark Streaming and Load to MongoDB

`KafkaToMongo.scala`
```scala
// ~~~~~~~~
// ~~~~~~~~
```
- Example Document in MongoDB

|   **KEY**  	|                 **VALUE**                	|
|----------	|----------------------------------------:	|
| _text_     	| _RT @RepThomasMassie: You’re at least 2_ 	|
| created_at 	| Wed Apr 06 15:55:03 +0000 2022           	|
| user_id    	| 980238526305460224                       	|

## STEP 4: Serve MongoDB data to Akka HTTP API on Localhost

`Server.scala`
```scala
// ~~~~~~~~~~~
// ~~~~~~~~~~~
```

Go to `localhost:8080/api/all`

**Result**
```json
[
  {
    "_id": {
      "oid": "624dd4a49898fd74d9963671"
    },
    "created_at": "Wed Apr 06 15:30:55 +0000 2022",
    "entities_hashtags": "[]",
    "id": "1511728162857619458",
    "lang": "en",
    "text": "@oscarhumb @LangmanVince Yeah Biden is a piece of sh*t liar and a failure!\n\nWhat kind of stupidty does it take to b… https://t.co/L3p8ncFeFa",
    "truncated": "true",
    "user_location": "fabulous Las Vegas, NV",
    "user_name": "A Devoted Yogi",
    "user_screen_name": "ADevotedYogi"
  },
  {
    "_id": {
      "oid": "624dd4a49898fd74d9963672"
    },
    "created_at": "Wed Apr 06 15:30:55 +0000 2022",
    "entities_hashtags": "[]",
    "id": "1511728163168174093",
    "lang": "fr",
    "text": "RT @Belzeboule_: @mev479 @marc_2969 @ch_coulon L'abrogation du pass est dans le programme de Zemmour. Et c'est bien une abrogation, pas une…",
    "truncated": "false",
    "user_location": "Montauban, France",
    "user_name": "GUERMACHE BHARIA",
    "user_screen_name": "GuermacheBharia"
  }
]
```
