import com.google.common.collect.Lists
import com.twitter.hbc.ClientBuilder
import com.twitter.hbc.core.Constants
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint
import com.twitter.hbc.core.processor.StringDelimitedProcessor
import com.twitter.hbc.httpclient.auth.OAuth1
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import scala.io.Source
import java.util.Properties
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}


object TwitterToKafka {

  @throws[InterruptedException]
  def run(apiKey: String, apiKeySecret: String, accessToken: String, accessTokenSecret: String): Unit = {

    // Create an appropriately sized blocking queue
    val queue = new LinkedBlockingQueue[String](10000)

    // Endpoint use to track terms
    val endpoint = new StatusesFilterEndpoint

    // Add some track terms
    // reading keywords from text file
    val filename = "src/main/resources/keywords.txt"
    var keywords= ""
    for (keyword <- Source.fromFile(filename).getLines) {
      keywords += keyword.toLowerCase + ", "
    }

    endpoint.trackTerms(Lists.newArrayList(keywords))

    // Define auth structure
    val auth = new OAuth1(apiKey, apiKeySecret, accessToken, accessTokenSecret)

    // Create a new BasicClient. By default gzip is enabled.
    val client = new ClientBuilder()
      .name("sampleExampleClient")
      .hosts(Constants.STREAM_HOST)
      .endpoint(endpoint)
      .authentication(auth)
      .processor(new StringDelimitedProcessor(queue))
      .build

    // Connect the client with the above configuration
    client.connect()

    // Set configuration for kafka
    val kafkaProducerProps: Properties = {
      val props = new Properties()
      props.put("bootstrap.servers", "localhost:9092")
      props.put("key.serializer", classOf[StringSerializer].getName)
      props.put("value.serializer", classOf[StringSerializer].getName)
      props
    }

    // Create a producer for kafka
    val producer = new KafkaProducer[String, String](kafkaProducerProps)

    // Take 3 messages from stream and push it to kafka topic named test-topic with null key
    for (msgRead <- 0 until 3) {
      val msg =  queue.poll(5, TimeUnit.SECONDS)
      print(msg)
      if (msg != null) {
        producer.send(new ProducerRecord[String, String]("test-topic", null, msg))
      }
    }

    // Stop the client
    client.stop()

    // Print no. of tweets fetched
    println(s"The client read ${client.getStatsTracker.getNumMessages - 1} messages!")
  }

  def main(args: Array[String]): Unit = {

    val config = ConfigFactory.load()
    val apiKey = config.getString("apiKey")
    val apiKeySecret = config.getString("apiKeySecret")
    val accessToken = config.getString("accessToken")
    val accessTokenSecret = config.getString("accessTokenSecret")

    System.setProperty("apiKey", apiKey)
    System.setProperty("apiKeySecret", apiKeySecret)
    System.setProperty("accessToken", accessToken)
    System.setProperty("accessTokenSecret", accessTokenSecret)
    try TwitterToKafka.run(apiKey, apiKeySecret, accessToken, accessTokenSecret)
    catch {
      case e: InterruptedException =>
        System.out.println(e)
    }
  }
}