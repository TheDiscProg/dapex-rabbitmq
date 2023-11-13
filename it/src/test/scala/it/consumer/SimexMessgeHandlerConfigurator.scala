package it.consumer

import cats.Applicative
import it.entites.TestRabbitQueue
import it.rmq.caching.CachingServiceAlgebra
import org.typelevel.log4cats.Logger
import simex.rabbitmq.consumer
import simex.rabbitmq.consumer.SimexMessageHandler

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
