import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}

object SparkMongo {
  // To remove INFO lines from terminal while running the code
  Logger.getLogger("org").setLevel(Level.OFF)
  // Fetch ALL Content from mongoDB
  def fetchAllJsonString(): String = {
    // Configure the session with MongoDB
    val spark = SparkSession.builder()
      .master("local")
      .appName("demo")
      .config("spark.mongodb.input.uri", "mongodb://localhost:27017/twitter_db.covid_tweets")
      .config("spark.mongodb.output.uri", "mongodb://localhost:27017/twitter_db.covid_tweets")
      .getOrCreate()
    import spark.implicits._

    val tweets_all: DataFrame = spark.read.format("com.mongodb.spark.sql.DefaultSource")
      .option("uri", "mongodb://localhost:27017/twitter_db.covid_tweets")
      .load()
    // tweets_all
    // "_id"             : {  "oid": "624dd4a49898fd74d9963671"},
    // "created_at"       : "Wed Apr 06 15:30:55 +0000 2022",
    // "entities_hashtags": "[]",
    // "id"               : "1511728162857619458",
    dataframeToJsonString(tweets_all)
  }
  def dataframeToJsonString(df: DataFrame): String = {
    val output_df = df.select(to_json(struct(col("*"))).alias("content"))
    // output_df
    //       content
    // {"_id":{"oid":"62"...
    // {"_id":{"oid":"63...
    val json_string: Array[Any] = output_df.select("content").rdd.map(r => r(0)).collect()
    // json_string
    // {"_id":{"oid":"624dd4a49898fd74d9963671"},"created_at":"Wed Apr 06 15:30:55 +0000 2022","entities_hashtags":"[]","id":"15117281
    //{"_id":{"oid":"624dd4a49898fd74d9963672"},"created_at":"Wed Apr 06 15:30:55 +0000 2022","entities_hashtags":"[]","id":"151172816
    json_string.mkString("[", ",", "]")  // [  {}, {}, {} ]

  }
  def fetchQueryJsonString(pipeline: String): String = {
    // Configure the session with MongoDB
    val spark = SparkSession.builder()
      .master("local")
      .appName("demo")
      .config("spark.mongodb.input.uri", "mongodb://localhost:27017/twitter_db.covid_tweets")
      .config("spark.mongodb.output.uri", "mongodb://localhost:27017/twitter_db.covid_tweets")
      .getOrCreate()
    import spark.implicits._
    val df = spark.read.format("com.mongodb.spark.sql.DefaultSource")
      .option("pipeline", pipeline)  // db.getCollection("covid_tweets").aggregate(pipeline)
      .option("uri","mongodb://localhost:27017/twitter_db.covid_tweets")
      .load()
    dataframeToJsonString(df)
  }
  def main(args: Array[String]): Unit = {
//    print(fetchAllJsonString())
  }
}
