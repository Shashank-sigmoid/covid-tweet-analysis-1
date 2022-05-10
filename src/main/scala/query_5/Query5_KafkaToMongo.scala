import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Column, DataFrame, SparkSession}

object Query5_KafkaToMongo {

  // Function to replace every "." to "_" in a string
  def replaceDotsForColName(name: String): String = {
    if(name.contains('.')){
      name.replace(".", "_")
    }
    else{
      name
    }
  }

  def main(args: Array[String]): Unit = {

    // Configure the session with MongoDB
    val spark = SparkSession.builder
      .master("local")
      .appName("demo")
      .config("spark.mongodb.output.uri", "mongodb://localhost:27017")
      .getOrCreate()

    // Add this for $ "value" i.e. explicitly mentioning that value is a attribute not string
    import spark.implicits._

    // Reading the stream from Kafka Topic (Source)
    // df will be in binary format b"------"
    val df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "who-tweet")
      .option("startingOffsets", "earliest")
      .load()

    val raw_json: DataFrame = df.selectExpr("CAST(value AS STRING)")

    // Mention column to be extracted from "value" column
    val columns: Seq[String] =  Seq(
      "created_at",
      "id",
      "full_text",
      "truncated",
      "user.name",
      "user.screen_name",
      "user.location",
      "entities.hashtags",
      "lang"
    )

    val cleaned_columns: Seq[Column] = columns
      .map(c => get_json_object($"value", s"$$.$c").alias(replaceDotsForColName(c)))

    // Table with null values
    val table_with_null_values: DataFrame = raw_json.select(cleaned_columns: _*)

    // Remove document which doesn't contain user_location and created_at
    val table = table_with_null_values.na.drop(Seq("user_location", "created_at"))

    // Write data related to WHO tweets into collection who_tweets
    table.writeStream.foreachBatch { (batchDF: DataFrame, batchId: Long) =>
      batchDF.write
        .format("mongo")
        .mode("append")
        .option("database", "twitter_db")
        .option("collection", "who_tweets")
        .save()
    }.start().awaitTermination()
  }
}