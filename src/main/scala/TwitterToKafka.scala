import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import com.typesafe.config.ConfigFactory
import twitter4j.{FilterQuery, StallWarning, Status, StatusDeletionNotice, StatusListener, TwitterStreamFactory, TwitterObjectFactory}
import twitter4j.conf.ConfigurationBuilder
import scala.io.Source
import java.util.Properties
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

object TwitterToKafka{

  @throws[InterruptedException]
  def run(apiKey: String, apiKeySecret: String, accessToken: String, accessTokenSecret: String): Unit = {

    // Reading keywords from txt file
    val filename = "src/main/resources/keywords.txt"
    var keywords: String = ""
    for(line <- Source.fromFile(filename).getLines) {
      keywords += line + ", "
    }

    // Create an appropriately sized blocking queue
    val queue = new LinkedBlockingQueue[String](10000)

    val cb = new ConfigurationBuilder()
      .setDebugEnabled(true)
      .setJSONStoreEnabled(true)
      .setOAuthConsumerKey(apiKey)
      .setOAuthConsumerSecret(apiKeySecret)
      .setOAuthAccessToken(accessToken)
      .setOAuthAccessTokenSecret(accessTokenSecret)
      .setTweetModeExtended(true)

    val twitterStream = new TwitterStreamFactory(cb.build()).getInstance()
    val listener = new StatusListener() {
      def onStatus(status: Status) {
        val result = TwitterObjectFactory.getRawJSON(status)
        queue.offer(result)
      }
      def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = {
        println("Got a status deletion notice ID: " + statusDeletionNotice.getStatusId)
      }

      def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = {
        println("Got track limitation notice: " + numberOfLimitedStatuses)
      }

      def onScrubGeo(userId: Long, upToStatusId: Long): Unit = {
        println("Got scrub_geo event userId: " + userId + "upToStatusId: " + upToStatusId)
      }

      def onStallWarning(warning: StallWarning): Unit = {
        println("Got stall warning: " + warning)
      }

      def onException(ex: Exception) {
        ex.printStackTrace
      }
    }
    twitterStream.addListener(listener)
    val query = new FilterQuery().track(keywords)
    twitterStream.filter(query)

    // Set configuration for kafka
    val kafkaProducerProps: Properties = {
      val props = new Properties()
      props.put("bootstrap.servers", "localhost:9092")
      props.put("acks", "all")
//      props.put("retries", 0)
      props.put("key.serializer", classOf[StringSerializer].getName)
      props.put("value.serializer", classOf[StringSerializer].getName)
      props
    }

    // Create a producer for kafka
    val producer = new KafkaProducer[String, String](kafkaProducerProps)

    // Take x messages from stream and push it to kafka topic named covid-tweet
    var r = 0
    for (_ <- 0 until 1000) {
      val msg =  queue.poll(5, TimeUnit.SECONDS)
      if (msg != null) {
        // println(msg)
        r += 1
        producer.send(new ProducerRecord[String, String]("covid-tweet", null, msg))
      }
    }
    print(s"No. of messages fetched: ${r}")
    producer.close()
    twitterStream.cleanUp()
    twitterStream.shutdown()
  }

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
//    val consumerKey = config.getString("apiKey")
//    val consumerSecret = config.getString("apiKeySecret")
//    val accessToken = config.getString("accessToken")
//    val accessTokenSecret = config.getString("accessTokenSecret")
    val accessToken: String="1509882730707775488-7Aa0roBpZ18YRHlwFAIQxwqvymWohb"
    val accessTokenSecret: String="E8oXSHCg9uXGIgxi5RvvfBRiVc95FsMjhfvmZhNNtR0u8"
    val consumerKey: String="4ep0QIoguktMBmWgvWHpBcKon"
    val consumerSecret: String="qO250zSrdGHmAdpKgM3s7PYCpTH835dzF6R8cKfYyWWgVC0bwZ"

    System.setProperty("apiKey", consumerKey)
    System.setProperty("apiKeySecret", consumerSecret)
    System.setProperty("accessToken", accessToken)
    System.setProperty("accessTokenSecret", accessTokenSecret)

    try TwitterToKafka.run(consumerKey, consumerSecret, accessToken, accessTokenSecret)
    catch {
      case e: InterruptedException =>
        System.out.println(e)
    }
  }
}
