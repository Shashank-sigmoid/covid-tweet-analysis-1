import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Column, DataFrame, SparkSession}

object Query5_KafkaToMongo {

  // Function to replace every "." to "_" in a string
  def replaceDotsForColName(name: String): String = {
    if (name.contains('.')) {
      name.replace(".", "_")
    }
    else {
      name
    }
  }

  // Function to configure the session with MongoDB in Docker
  def configureSpark(): SparkSession = {
    val spark = SparkSession.builder
      .master("local")
      .appName("demo")
      .config("spark.mongodb.input.uri", "mongodb://root:root@mongo:27017")
      .config("spark.mongodb.output.uri", "mongodb://root:root@mongo:27017")
      .getOrCreate()
    spark
  }

  // Function to take dataFrame (Streaming) as input and writes it to given database and collection
  def writeDataframeToMongo(table: DataFrame, database_name: String, collection_name: String): Unit = {
    table.writeStream.foreachBatch { (batchDF: DataFrame, batchId: Long) =>
      batchDF.write
        .format("mongo")
        .mode("append")
        .option("database", database_name)
        .option("collection", collection_name)
        .save()
    }.start().awaitTermination(10000)
  }

  def readStreamFromKafka(spark: SparkSession, topic_name: String): DataFrame = {
    // Subscribing to the Kafka-topic in Docker and reading the message stream (Source)
    //     df
    //     KEY                VALUE
    //    "value"         |  { "$binary" : "eyJjcmVhdGVjU5MDU1MjMzIn0NCg==", "$type" : "00" },
    //    "topic"         | "test-topic",
    //    "partition"     |  0,
    //    "offset"        |  NumberLong(0),
    //    "timestamp"     |  ISODate("2022-04-06T15:31:00.696Z"),
    //    "timestampType" |  0
    val df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "kafka:9092")
      .option("subscribe", topic_name)
      .option("startingOffsets", "earliest")
      .load()

    // raw_json
    // KEY                 VALUE
    //             v this \(backslash) is to let json know to take " (double quote) literally and not as elimination of string
    // "value"  | "{\"created_at\":\"Wed Apr 06 15:30:55 +0000 2022\",\"id\":1511728162857619458,\"id_str\":\"1511728162857619458\",
    val raw_json: DataFrame = df.selectExpr("CAST(value AS STRING)")
    raw_json
  }

  def filterColumnsFromDataframe(spark: SparkSession, raw_json: DataFrame, columns: Seq[String]): DataFrame = {
    // get_json_object(col, path) => Extracts json object from a json string based on json path specified,
    //                               and returns json string of the extracted json object.
    // path = "$$.$c" => First $ specifies second $ as variable not str, now one $ implies root document in MongoDB
    //                   Dot (.) implies go inside root document and find the value of key written right after it ($c)
    //                   Third dollar specifies c as variable not str which is `columns` values one by one
    // .alias(fun(c)) => Because MongoDB doesn't support (.) or ($) in its column name, so if any value in columns contain
    //                   data like `user.id` (which means go inside `user` and extract value of key `id`), we have to
    //                    replace every dots with _ (underscore)
    import spark.implicits._
    val cleaned_columns: Seq[Column] = columns
      .map(c => get_json_object($"value", s"$$.$c").alias(replaceDotsForColName(c)))

    // Select all columns which is mentioned in the plan of cleaned_columns [PLAN]
    // cleaned_columns is a PLAN, not an actual collection
    // We can't actually collect data in streaming data, we can only use `data.write` to collect it, which will be used as sink
    // $"*" +:  => Add this before cleaned_columns to get value columns as well
    val table_with_potential_null_values: DataFrame = raw_json.select(cleaned_columns: _*)
    table_with_potential_null_values
  }

  // Function to remove the document which doesn't contain user_location and created_at
  def removeNullFromDataframe(table_with_potential_null_values: DataFrame): DataFrame = {
    val table = table_with_potential_null_values.na.drop(Seq("user_location", "created_at"))
    table
  }

  def main(args: Array[String]): Unit = {
    val spark = configureSpark()
    val raw_json = readStreamFromKafka(spark, "who-tweet")
    val table_with_potential_null_values = filterColumnsFromDataframe(spark, raw_json, Seq(
      "created_at",
      "id",
      "full_text",
      "truncated",
      "user.name",
      "user.screen_name",
      "user.location",
      "entities.hashtags",
      "lang"
    ))
    val table = removeNullFromDataframe(table_with_potential_null_values)
    writeDataframeToMongo(table, "twitter_db", "who_tweets")
  }
}