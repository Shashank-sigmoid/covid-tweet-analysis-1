import com.google.common.collect.Lists
import com.twitter.hbc.ClientBuilder
import com.twitter.hbc.core.Constants
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint
import com.twitter.hbc.core.processor.StringDelimitedProcessor
import com.twitter.hbc.httpclient.auth.OAuth1
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.spark.sql.SparkSession

import java.util.Properties
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}


object TwitterToKafka {

  @throws[InterruptedException]
  def run(consumerKey: String, consumerSecret: String, token: String, secret: String): Unit = {
    // Create an appropriately sized blocking queue
    val queue = new LinkedBlockingQueue[String](10000)
    // Endpoint use to track terms
    val endpoint = new StatusesFilterEndpoint
    // add some track terms
    // todo: #1 - Take the terms from the file keywords.txt
    endpoint.trackTerms(Lists.newArrayList("covid", "virus"))
    // Define auth structure
    val auth = new OAuth1(consumerKey, consumerSecret, token, secret)
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

    // Take 10 msgs from stream and push it to kafka topic named test-topic
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
    println("The client read %d messages!\n", client.getStatsTracker.getNumMessages - 1)
  }

  def main(args: Array[String]): Unit = {
    val access_token: String="<your_access_token>"
    val access_token_secret: String="<your_access_token_secret>"
    val consumer_key: String="<your_consumer_key>"
    val consumer_secret: String="<your_consumer_secret>"
    try TwitterToKafka.run(consumer_key, consumer_secret, access_token, access_token_secret)
    catch {
      case e: InterruptedException =>
        System.out.println(e)
    }
  }
}
