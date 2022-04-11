import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._

object ScalaMongo{
  def main(args: Array[String]): Unit = {
    // Uri for mongo
    val uri: String = "mongodb://localhost:27017"
    // Set property
    System.setProperty("org.mongodb.async.type", "netty")
    // Get Mongo Client
    val client: MongoClient = MongoClient(uri)
    // Get Database
    val db: MongoDatabase = client.getDatabase("twitter_db")
    // Get Collection
    val collection = db.getCollection("covid_tweets")
    // Print Results of collection
    val observable = collection.find()
//    println(x)
    observable.subscribe ( new Observer[Document] {
      override def onNext(result: Document): Unit = println(result.toJson())
      override def onError(e: Throwable): Unit = println("Failed" + e.getMessage)
      override def onComplete(): Unit = println("Completed")
    })
    val resultSeq = observable.collect
    Thread.sleep(5000)
//    resultSeq.foreach(x => println(x))

  }

}