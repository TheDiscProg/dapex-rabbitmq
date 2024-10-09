package io.github.thediscprog.simexrabbitmq.it

import cats.effect._
import cats.effect.std.Dispatcher
import com.github.blemale.scaffeine.{Cache, Scaffeine}
import io.circe.config.parser
import io.github.thediscprog.simexmessaging.messaging.Simex
import io.github.thediscprog.simexrabbitmq.config.RabbitMQConfig
import io.github.thediscprog.simexrabbitmq.consumer.SimexMessageHandler
import io.github.thediscprog.simexrabbitmq.it.consumer.SimexMessgeHandlerConfigurator
import io.github.thediscprog.simexrabbitmq.it.entites.Service
import io.github.thediscprog.simexrabbitmq.it.rmq.caching.{
  CachingServiceAlgebra,
  ScaffeineCachingService
}
import org.typelevel.log4cats.{Logger => Log4CatsLogger}

object RMQService {

  def setUpRMQService[F[_]: Async: Log4CatsLogger](): Resource[F, Service[F]] =
    for {
      rmqConf: RabbitMQConfig <- Resource.eval(parser.decodePathF[F, RabbitMQConfig]("rabbitMQ"))
      rmqDispatcher: Dispatcher[F] <- Dispatcher.parallel

      cache: Cache[String, Simex] =
        Scaffeine()
          .recordStats()
          .build[String, Simex]()
      cachingService: CachingServiceAlgebra[F] = new ScaffeineCachingService[F](cache)

      queueHandlers: Vector[SimexMessageHandler[F]] = SimexMessgeHandlerConfigurator.getHandlers(
        cachingService
      )
    } yield Service[F](rmqConf, rmqDispatcher, queueHandlers.toList, cachingService)
}
