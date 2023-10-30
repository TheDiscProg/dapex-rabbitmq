package dapex.rabbitmq.it

import cats.effect._
import cats.effect.std.Dispatcher
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import dapex.messaging.DapexMessage
import dapex.rabbitmq.Rabbit
import dapex.rabbitmq.config.RabbitMQConfig
import dapex.rabbitmq.consumer.DapexMessageHandler
import dapex.rabbitmq.it.consumer.DapexMessgeHandlerConfigurator
import dapex.rabbitmq.it.entites.Service
import dapex.rabbitmq.it.rmq.caching.ScaffeineCachingService
import dapex.rabbitmq.publisher.DapexMQPublisher
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import dev.profunktor.fs2rabbit.model.AMQPChannel
import io.circe.config.parser
import org.typelevel.log4cats.{Logger => Log4CatsLogger}

object RMQService {

  def setUpRMQService[F[_]: Async: Log4CatsLogger](): Resource[F, Service[F]] =
    for {
      rmqConf: RabbitMQConfig <- Resource.eval(parser.decodePathF[F, RabbitMQConfig]("rabbitMQ"))
      rmqDispatcher <- Dispatcher.parallel
      rmqClient: RabbitClient[F] <- Resource.eval(Rabbit.getRabbitClient(rmqConf, rmqDispatcher))
      aMQPChannel: AMQPChannel <- rmqClient.createConnectionChannel

      cache: Cache[String, DapexMessage] =
        Scaffeine()
          .recordStats()
          .build[String, DapexMessage]()
      cachingService = new ScaffeineCachingService[F](cache)

      queueHandlers: Vector[DapexMessageHandler[F]] = DapexMessgeHandlerConfigurator.getHandlers(
        cachingService
      )
      rmqPublisher = new DapexMQPublisher(rmqClient)

    } yield Service[F](
      rmqClient = rmqClient,
      handlers = queueHandlers.toList,
      rmqPublisher = rmqPublisher,
      channel = aMQPChannel,
      cachingService = cachingService
    )
}
