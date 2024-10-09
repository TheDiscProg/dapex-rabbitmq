package io.github.thediscprog.simexrabbitmq.it.consumer

import cats.Applicative
import io.github.thediscprog.simexrabbitmq.consumer.SimexMessageHandler
import io.github.thediscprog.simexrabbitmq.it.entites.TestRabbitQueue
import io.github.thediscprog.simexrabbitmq.it.rmq.caching.CachingServiceAlgebra
import org.typelevel.log4cats.Logger

object SimexMessgeHandlerConfigurator {

  def getHandlers[F[_]: Applicative: Logger](
      cachingService: CachingServiceAlgebra[F]
  ): Vector[SimexMessageHandler[F]] = {
    val consumer = RMQConsumer(cachingService)
    Vector(
      SimexMessageHandler(TestRabbitQueue.SERVICE_AUTHENTICATION_QUEUE, consumer.handleAuthQueue),
      SimexMessageHandler(TestRabbitQueue.SERVICE_DBREAD_QUEUE, consumer.handleDBReadQueue),
      SimexMessageHandler(
        TestRabbitQueue.SERVICE_COLLECTION_POINT_QUEUE,
        consumer.handleCollectionQueue
      )
    )
  }
}
