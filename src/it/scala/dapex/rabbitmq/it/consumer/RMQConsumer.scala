package dapex.rabbitmq.it.consumer

import cats.Applicative
import dapex.messaging.DapexMessage
import dapex.rabbitmq.it.rmq.caching.CachingServiceAlgebra
import org.typelevel.log4cats.Logger
import cats.implicits._

class RMQConsumer[F[_]: Applicative: Logger](cachingService: CachingServiceAlgebra[F]) {

  def handleAuthQueue(msg: DapexMessage): F[Unit] =
    logMessage("Authentication", msg)

  def handleDBReadQueue(msg: DapexMessage): F[Unit] =
    logMessage("DBRead", msg)

  def handleCollectionQueue(msg: DapexMessage): F[Unit] =
    logMessage("Collection Point", msg)

  private def logMessage(queue: String, msg: DapexMessage): F[Unit] = {
    Logger[F].info(
      s"Received message: [Queue $queue], [Endpoint: ${msg.endpoint}], [Method: ${msg.endpoint.method}], [ID: ${msg.client.requestId}]"
    )
    cachingService.storeInCache(msg.client.requestId, msg) *> ().pure[F]
  }

}

object RMQConsumer {
  def apply[F[_]: Applicative: Logger](cachingService: CachingServiceAlgebra[F]): RMQConsumer[F] =
    new RMQConsumer[F](cachingService)
}
