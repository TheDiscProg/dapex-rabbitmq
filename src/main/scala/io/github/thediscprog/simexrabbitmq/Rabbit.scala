package io.github.thediscprog.simexrabbitmq

import cats.effect.Async
import cats.effect.std.Dispatcher
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import io.github.thediscprog.simexrabbitmq.config.RabbitMQConfig

object Rabbit {

  def getRabbitClient[F[_]: Async](
      conf: RabbitMQConfig,
      dispatcher: Dispatcher[F]
  ): F[RabbitClient[F]] =
    RabbitClient
      .default(conf.asFs2RabbitConfig)
      .build(dispatcher)

}
