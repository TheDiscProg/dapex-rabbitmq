package it.consumer

import cats.Applicative
import org.typelevel.log4cats.Logger
import cats.implicits._
import it.rmq.caching.CachingServiceAlgebra
import simex.messaging.Simex

class RMQConsumer[F[_]: Applicative: Logger](cachingService: CachingServiceAlgebra[F]) {

  def handleAuthQueue(msg: Simex): F[Unit] =
    logMessage("Authentication", msg)

  def handleDBReadQueue(msg: Simex): F[Unit] =
    logMessage("DBRead", msg)

  def handleCollectionQueue(msg: Simex): F[Unit] =
    logMessage("Collection Point", msg)

  private def logMessage(queue: String, msg: Simex): F[Unit] = {
    Logger[F].info(
      s"Received message: [Queue $queue], [Destination: ${msg.destination}], [Method: ${msg.destination.method}], [ID: ${msg.client.requestId}]"
    )
    cachingService.storeInCache(msg.client.requestId, msg) *> ().pure[F]
  }

}

object RMQConsumer {
  def apply[F[_]: Applicative: Logger](cachingService: CachingServiceAlgebra[F]): RMQConsumer[F] =
    new RMQConsumer[F](cachingService)
}
