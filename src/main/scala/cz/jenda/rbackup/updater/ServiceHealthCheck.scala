package cz.jenda.rbackup.updater
import com.softwaremill.sttp._
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration._
import scala.util._

object ServiceHealthCheck extends StrictLogging {
  private implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  def getStatus: Try[Unit] = {
    val request = sttp.get(uri"http://localhost:3370/status").readTimeout(2.seconds)
    val response = request.send()

    logger.debug(s"Service response: $response")

    if (response.code == StatusCodes.Ok && response.unsafeBody == "ok") {
      Success(())
    } else {
      Failure(new IllegalStateException(s"HTTP ${response.code}: ${response.unsafeBody}"))
    }
  }
}
