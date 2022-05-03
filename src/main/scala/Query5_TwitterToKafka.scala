import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import twitter4j.{Paging, TwitterException, TwitterFactory, TwitterObjectFactory}
import twitter4j.conf.ConfigurationBuilder

import java.util.Properties
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

object Query5_TwitterToKafka {

  def run(apiKey: String, apiKeySecret: String, accessToken: String, accessTokenSecret: String): Unit = {

    // Build configurations
    val cb = new ConfigurationBuilder()
      .setDebugEnabled(true)
      .setJSONStoreEnabled(true)
      .setOAuthConsumerKey(apiKey)
      .setOAuthConsumerSecret(apiKeySecret)
      .setOAuthAccessToken(accessToken)
      .setOAuthAccessTokenSecret(accessTokenSecret)
      .setTweetModeExtended(true)

    // Create an appropriately sized blocking queue
    val queue = new LinkedBlockingQueue[String](10000)

    // Set configuration for kafka
    val kafkaProducerProps: Properties = {
      val props = new Properties()
      props.put("bootstrap.servers", "localhost:9092")
      props.put("acks", "all")
      props.put("retries", 0)
      props.put("key.serializer", classOf[StringSerializer].getName)
      props.put("value.serializer", classOf[StringSerializer].getName)
      props
    }

    // Create a producer for kafka
    val producer = new KafkaProducer[String, String](kafkaProducerProps)
    val twitter = new TwitterFactory(cb.build()).getInstance()
    try {

      var page = 250
      for (_ <- page until 500) {

        // Page determines how old tweet would be fetched, Count determines the no. of tweets to be fetched
        val paging = new Paging(page, 5)

        // Fetching tweets from WHO only (Twitter ID of WHO => 14499829)
        val statuses = twitter.getUserTimeline(14499829, paging)
        var i = 0
        for (_ <- i until statuses.size()) {

          val result = TwitterObjectFactory.getRawJSON(statuses.get(i))
          queue.offer(result)
          i += 1
        }
        page += 1
      }
      val sz = queue.size()
      for (_ <- 0 until queue.size()) {
        val msg = queue.poll(5, TimeUnit.SECONDS)
        if (msg != null) {
          println(msg)
          producer.send(new ProducerRecord[String, String]("who-tweet",null, msg))
        }
      }
      print(s"No. of messages fetched: ${sz}")
    }
    catch {
      case te: TwitterException =>
        te.printStackTrace()
        println("Failed to get timeline: " + te.getMessage)
        System.exit(-1)
    }
  }

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val consumerKey = config.getString("apiKey")
    val consumerSecret = config.getString("apiKeySecret")
    val accessToken = config.getString("accessToken")
    val accessTokenSecret = config.getString("accessTokenSecret")

    System.setProperty("apiKey", consumerKey)
    System.setProperty("apiKeySecret", consumerSecret)
    System.setProperty("accessToken", accessToken)
    System.setProperty("accessTokenSecret", accessTokenSecret)

    try Query5_TwitterToKafka.run(consumerKey, consumerSecret, accessToken, accessTokenSecret)
    catch {
      case e: InterruptedException =>
        println(e)
    }
  }
}