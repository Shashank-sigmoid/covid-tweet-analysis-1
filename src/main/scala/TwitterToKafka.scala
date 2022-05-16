import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import twitter4j._
import twitter4j.conf.ConfigurationBuilder

import java.nio.charset.CodingErrorAction
import java.util.Properties
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import scala.io.Codec
import scala.io.Source

/*
    Takes input as authentication strings for TwitterAPI
    and push the streaming tweets (filtered) into Kafka Topic
*/
object TwitterToKafka{

  @throws[InterruptedException]
  def run(apiKey: String, apiKeySecret: String, accessToken: String, accessTokenSecret: String): Unit = {

    // Defining codec for Source.fromFile to get data in specified format else (MalformedInputException)
    // https://stackoverflow.com/questions/10846848/why-do-i-get-a-malformedinputexception-from-this-code
    implicit val codec = Codec("UTF-8")
    codec.onMalformedInput(CodingErrorAction.REPLACE)
    codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

    // Reading keywords related to coronavirus from txt file
    val filename = "src/main/resources/keywords.txt"
    // `keywords` will contain appropriate format from keywords to pass while tracking and selecting specific tweets
    var keywords: String = ""
    for(line <- Source.fromFile(filename).getLines) {
      keywords += line + ", "
    }
    //TODO: add resources to fat jar: resources not recognized
//      var keywords: String = "corona, coronavirus, covid"

    // Create an appropriately sized blocking custom queue
    val queue = new LinkedBlockingQueue[String](10000)

    val cb = new ConfigurationBuilder()
      .setDebugEnabled(true)              // Enables debug output
      .setJSONStoreEnabled(true)          // raw JSON forms will be stored in DataObjectFactory.
      .setOAuthConsumerKey(apiKey)
      .setOAuthConsumerSecret(apiKeySecret)
      .setOAuthAccessToken(accessToken)
      .setOAuthAccessTokenSecret(accessTokenSecret)
      .setTweetModeExtended(true) 

    // Creates a TwitterStreamFactory with the predefined `cb` configuration.
    val twitterStream = new TwitterStreamFactory(cb.build()).getInstance()

    // Create a Listener for the Stream 
    val listener = new StatusListener() {

      // For every status change, add result in the queue (if not full)
      def onStatus(status: Status) {
        val result = TwitterObjectFactory.getRawJSON(status)
        queue.offer(result)
      }

      // This notice will be sent each time a limited stream becomes unlimited
      def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = {
        println("Got a status deletion notice ID: " + statusDeletionNotice.getStatusId)
      }

      // Called upon location deletion messages.
      def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = {
        println("Got track limitation notice: " + numberOfLimitedStatuses)
      }
      // Called when receiving stall warnings.
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

    // Add listener to the Stream
    twitterStream.addListener(listener)
    // Create a FilterQuery based on keywords (related to coronavirus)
    val query = new FilterQuery().track(keywords)
    // Apply the filter to the stream
    twitterStream.filter(query)

    // Set configuration for kafka
    val kafkaProducerProps: Properties = {
      val props = new Properties()
      props.put("bootstrap.servers", "localhost:9092")
      props.put("acks", "all")
      props.put("key.serializer", classOf[StringSerializer].getName)
      props.put("value.serializer", classOf[StringSerializer].getName)
      props
    }

    // Create a producer for kafka
    val producer = new KafkaProducer[String, String](kafkaProducerProps)

    // Take x messages from stream and push it to kafka topic named `covid-tweet`
    var r = 0
    for (_ <- 0 until 1000) {
      // Poll the message from the queue with timeout of 5 seconds
      val msg =  queue.poll(5, TimeUnit.SECONDS)
      if (msg != null) {
         println(msg)
        r += 1
        // Create a new producer record and send to kafka with topic: "covid-tweet", key: "null", message: "message"
        producer.send(new ProducerRecord[String, String]("covid-tweet", null, msg))
      }
    }

    print(s"No. of messages fetched: ${r}")
    // Close the producer connected to Kafka Topic
    producer.close()
    // Clean and Shutdown internal stream consuming thread
    twitterStream.cleanUp()
    twitterStream.shutdown()
  }

  def main(args: Array[String]): Unit = {
    // By default, the ConfigFactory looks for a configuration file called application.properties
    val config = ConfigFactory.load()

    // Load the secrets from application.properties
    val apiKey = config.getString("apiKey")
    val apiKeySecret = config.getString("apiKeySecret")
    val accessToken = config.getString("accessToken")
    val accessTokenSecret = config.getString("accessTokenSecret")

    // Set the property in the running JVM instance
    System.setProperty("apiKey", apiKey)
    System.setProperty("apiKeySecret", apiKeySecret)
    System.setProperty("accessToken", accessToken)
    System.setProperty("accessTokenSecret", accessTokenSecret)

    // Start transferring the tweets from Twitter API to Kafka Topic
    try TwitterToKafka.run(apiKey, apiKeySecret, accessToken, accessTokenSecret)
    catch {
      case e: InterruptedException =>
        System.out.println(e)
    }
  }
}
