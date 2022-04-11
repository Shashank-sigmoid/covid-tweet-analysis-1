// https://riptutorial.com/akka/example/31018/akka-http-server--hello-world--scala-dsl-
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Server extends App {

  implicit val system: ActorSystem = ActorSystem("ProxySystem")

  val route = pathPrefix("api"){
    concat(
      get{
        path("hello"){
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      },
      get{
        path("tello"){
          complete(HttpEntity(ContentTypes.`application/json`, "[{\"name\": \"Bhavesh\", \"age\":21, \"gender\": \"Male\"},{\"name\": \"Bhavesh\", \"age\":21, \"gender\": \"Male\"}]"))
        }
      }
    )

  }

  val bindingFuture = Http().newServerAt("127.0.0.1", port = 8080).bindFlow(route)
  Await.result(system.whenTerminated, Duration.Inf)

}