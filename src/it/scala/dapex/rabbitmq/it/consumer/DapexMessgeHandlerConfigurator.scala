package dapex.rabbitmq.it.consumer

import cats.Applicative
import dapex.rabbitmq.consumer.DapexMessageHandler
import dapex.rabbitmq.it.entites.TestRabbitQueue
import dapex.rabbitmq.it.rmq.caching.CachingServiceAlgebra
import org.typelevel.log4cats.Logger

object DapexMessgeHandlerConfigurator {

  def getHandlers[F[_]: Applicative: Logger](
      cachingService: CachingServiceAlgebra[F]
  ): Vector[DapexMessageHandler[F]] = {
    val consumer = RMQConsumer(cachingService)
    Vector(
      DapexMessageHandler(TestRabbitQueue.SERVICE_AUTHENTICATION_QUEUE, consumer.handleAuthQueue),
      DapexMessageHandler(TestRabbitQueue.SERVICE_DBREAD_QUEUE, consumer.handleDBReadQueue),
      DapexMessageHandler(
        TestRabbitQueue.SERVICE_COLLECTION_POINT_QUEUE,
        consumer.handleCollectionQueue
      )
    )
  }
}
