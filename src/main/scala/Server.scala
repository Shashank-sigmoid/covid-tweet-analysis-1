import SparkMongo.{fetchAllJsonString, fetchQueryJsonString}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
object Server extends App {

  implicit val system: ActorSystem = ActorSystem("ProxySystem")

  // Pipeline for mongo query
  val pipeline: String = "[{ $match: { user_location: { $exists: true } } }]"

  // Route setup for localhost
  val route = pathPrefix("api"){
    concat(
      // Welcome Page
      get{
        path("hello"){
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      },
      // Fetch all content from database
      get{
        path("all"){
          complete(HttpEntity(ContentTypes.`application/json`, fetchAllJsonString()))
        }
      },
      // Fetch content based on pipeline
      get{
        path("query"){
          complete(HttpEntity(ContentTypes.`application/json`, fetchQueryJsonString(pipeline)))
        }
      }
    )

  }
  // Bind route at localhost:8080
  val bindingFuture = Http().newServerAt("127.0.0.1", port = 8080).bindFlow(route)
  // Wait for infinite second before terminating
  Await.result(system.whenTerminated, Duration.Inf)

}