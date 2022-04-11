import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
//import org.mongodb.scala.Document
import org.apache.spark.sql.SparkSession

object SparkMongo {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.OFF)
    // Configure the session with MongoDB
    val spark = SparkSession.builder()
      .master("local")
      .appName("demo")
      .config("spark.mongodb.input.uri", "mongodb://localhost:27017/twitter_db.covid_tweets")
      .config("spark.mongodb.output.uri", "mongodb://localhost:27017/twitter_db.covid_tweets")
      .getOrCreate()

//    println(spark)
    val ratings = spark.read.format("com.mongodb.spark.sql.DefaultSource").option("uri", "mongodb://localhost:27017/twitter_db.covid_tweets").load()
    ratings.show()
  }
}
