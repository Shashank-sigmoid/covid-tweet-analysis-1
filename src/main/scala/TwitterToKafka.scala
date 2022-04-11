import com.google.common.collect.Lists
import com.twitter.hbc.ClientBuilder
import com.twitter.hbc.core.Constants
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint
import com.twitter.hbc.core.processor.StringDelimitedProcessor
import com.twitter.hbc.httpclient.auth.OAuth1
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import scala.io.Source
//import org.apache.spark.sql.SparkSession

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
    // todo: #1 - Take the terms from the file keywords.txt
    // Reading keywords from txt file
    val filename = "keywords.txt"
    var keywords: String = ""
    for(line <- Source.fromFile(filename).getLines) {
      keywords += line + ", "
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

    // Take 10 messages from stream and push it to kafka topic named test-topic
    for (msgRead <- 0 until 3) {
        val msg =  queue.poll(5, TimeUnit.SECONDS)
        print(msg)
        if (msg != null) {
          producer.send(new ProducerRecord[String, String]("test-topic", null, msg))
        }
    }

    // Stop the client
    client.stop()

    // Print some stats
    println(s"The client read ${client.getStatsTracker.getNumMessages - 1} messages!")
  }

  def main(args: Array[String]): Unit = {
    val access_token = System.getenv("ACCESS_TOKEN")
    val access_token_secret = System.getenv("ACCESS_TOKEN_SECRET")
    val api_key: String = System.getenv("API_KEY")
    val api_key_secret = System.getenv("API_KEY_SECRET")
    try TwitterToKafka.run(api_key, api_key_secret, access_token, access_token_secret)
    catch {
      case e: InterruptedException =>
        System.out.println(e)
    }
  }
}
