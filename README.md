# Covid Tweet Analysis

# Problem Statement

[Link](https://docs.google.com/document/d/1u7evaLRQ_CFOKFJSi0JniaaI_4hoxFYAyMqHrtSwFuY/edit)

# Architecture

![structure](./images/structure.png)

### This repository is made to create the JAR file which is required for airflow deployment.

### The configurations of all the files is according to the airflow deployment.

## Steps required to follow to create the JAR file:

1. Install sbt in your system
```terminal
brew install sbt
```

2. Change the bootstrap servers in TwitterToKafka and Query5_TwitterToKafka files from localhost:9092 to kafka:9092
```scala
props.put("bootstrap.servers", "kafka:9092")
```

3. Change the spark configuration session builder in KafkaToMongo and Query5_KafkaToMongo according to the mongoDB container name
```scala
  .config("spark.mongodb.input.uri", "mongodb://root:root@mongo:27017")
  .config("spark.mongodb.output.uri", "mongodb://root:root@mongo:27017")
```

4. Navigate to the directory of the project through terminal and write the command
```terminal
sbt clean assembly
```

This command will create a JAR file which includes all the files, folders, library dependencies, etc. <br/>
This JAR file can take some time to create (around 5 minutes) and is quite large (200+ Mbs) <br/>
So, it is better not to be pushed in the remote repository.

5. To execute the object present in the JAR file, run the below command in the terminal
```terminal
java -cp <JAR_file_name> <object_name> 
```

6. The above command would be used to run the JAR file in airflow using BashOperator