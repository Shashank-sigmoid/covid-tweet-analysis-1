import org.apache.spark.SparkContext._
import org.apache.spark._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming._
import org.apache.spark.streaming.twitter._

import scala.util.parsing.json._

object KafkaToMongo {

  def main(args: Array[String]): Unit = {
    // Configure the session with MongoDB
    val spark = SparkSession.builder
                  .master("local")
                  .appName("demo")
                  .config("spark.mongodb.output.uri", "mongodb://localhost:27017")
                  .getOrCreate()
    // Add this for $"value" i.e. explicitly mentioning that value is a attribute not string
    import spark.implicits._
    // READ the stream from Kafka Topic (Source)
    // df will be in binary format b"------"
    val df = spark.readStream
                  .format("kafka")
                  .option("kafka.bootstrap.servers", "localhost:9092")
                  .option("subscribe", "test-topic")
                  .option("startingOffsets", "earliest")
                  .load()
    // raw_df will give single column value and single row as string {"tweet_id": ____"}
    val raw_json = df.selectExpr("CAST(value AS STRING)")
    // Schema for converting raw_json to dataframe
    val schema = new StructType()
      .add("tweet_id", StringType, nullable = true)
      .add("created_at",StringType, nullable = true)
      .add("text", StringType, nullable = true)
      .add("screen_name", StringType, nullable = true)
      .add("user_created_at", StringType, nullable = true)
      .add("user_location", StringType, nullable = true)
      .add("user_id", StringType, nullable = true)
      .add("geo", StringType, nullable = true)
      .add("is_truncated", StringType, nullable = true)
      .add("tweet_contributors", StringType, nullable = true)
      .add("place", StringType, nullable = true)
      .add("coordinates", StringType, nullable = true)


    // Convert raw_json using schema into a dataframe
    // .select("created_at", "tweet_id")
    val table = raw_json.select(from_json($"value",schema)
      .alias("tmp"))
      .select("tmp.*")

    // For each batch(batchDF), provided what should be done inside it as a function
    // Storing in mongo, with awaitTermination
    table.writeStream.foreachBatch { (batchDF: DataFrame, batchId: Long) =>
      batchDF.write
      .format("mongo")
      .mode("append")
      .option("database", "twitter_db")
      .option("collection", "covid_tweets")
      .save()
    }.start().awaitTermination()
  }
}
