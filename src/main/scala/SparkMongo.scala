import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.functions.{col, struct, to_json}
import org.apache.spark.sql.{DataFrame, SparkSession}
//import org.mongodb.scala.Document
import org.apache.spark.sql.SparkSession
object SparkMongo {
  // Fetch ALL Content from mongoDB
  def fetchAllJsonString(): String = {
    Logger.getLogger("org").setLevel(Level.OFF)
    // Configure the session with MongoDB
    val spark = SparkSession.builder()
      .master("local")
      .appName("demo")
      .config("spark.mongodb.input.uri", "mongodb://localhost:27017/twitter_db.covid_tweets")
      .config("spark.mongodb.output.uri", "mongodb://localhost:27017/twitter_db.covid_tweets")
      .getOrCreate()
    import spark.implicits._
    val tweets: DataFrame = spark.read.format("com.mongodb.spark.sql.DefaultSource").option("uri", "mongodb://localhost:27017/twitter_db.covid_tweets").load()
    val output_df = tweets.select(to_json(struct(col("*"))).alias("content"))
    val x = output_df.select("content").rdd.map(r => r(0)).collect()
    x.mkString("[", ",", "]")
  }
  def main(args: Array[String]): Unit = {

  }
}
