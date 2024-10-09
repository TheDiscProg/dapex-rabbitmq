package io.github.thediscprog.simexrabbitmq.it.entites

import cats.effect.std.Dispatcher
import io.github.thediscprog.simexrabbitmq.config.RabbitMQConfig
import io.github.thediscprog.simexrabbitmq.consumer.SimexMessageHandler
import io.github.thediscprog.simexrabbitmq.it.rmq.caching.CachingServiceAlgebra

case class Service[F[_]](
    rmqConf: RabbitMQConfig,
    rmqDispatcher: Dispatcher[F],
    handlers: List[SimexMessageHandler[F]],
    cachingService: CachingServiceAlgebra[F]
)
