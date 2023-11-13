package it

import cats.effect._
import cats.effect.std.Dispatcher
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import dev.profunktor.fs2rabbit.model.AMQPChannel
import io.circe.config.parser
import it.consumer.SimexMessgeHandlerConfigurator
import it.entites.Service
import it.rmq.caching.ScaffeineCachingService
import org.typelevel.log4cats.{Logger => Log4CatsLogger}
import simex.messaging.Simex
import simex.rabbitmq.Rabbit
import simex.rabbitmq.config.RabbitMQConfig
import simex.rabbitmq.consumer.SimexMessageHandler
import simex.rabbitmq.publisher.SimexMQPublisher

object RMQService {

  def setUpRMQService[F[_]: Async: Log4CatsLogger](): Resource[F, Service[F]] =
    for {
      rmqConf: RabbitMQConfig <- Resource.eval(parser.decodePathF[F, RabbitMQConfig]("rabbitMQ"))
      rmqDispatcher <- Dispatcher.parallel
      rmqClient: RabbitClient[F] <- Resource.eval(Rabbit.getRabbitClient(rmqConf, rmqDispatcher))
      aMQPChannel: AMQPChannel <- rmqClient.createConnectionChannel

      cache: Cache[String, Simex] =
        Scaffeine()
          .recordStats()
          .build[String, Simex]()
      cachingService = new ScaffeineCachingService[F](cache)

      queueHandlers: Vector[SimexMessageHandler[F]] = SimexMessgeHandlerConfigurator.getHandlers(
        cachingService
      )
      rmqPublisher = new SimexMQPublisher(rmqClient)

    } yield Service[F](
      rmqClient = rmqClient,
      handlers = queueHandlers.toList,
      rmqPublisher = rmqPublisher,
      channel = aMQPChannel,
      cachingService = cachingService
    )
}
