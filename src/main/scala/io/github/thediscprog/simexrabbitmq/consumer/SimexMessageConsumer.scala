package io.github.thediscprog.simexrabbitmq.consumer

import dev.profunktor.fs2rabbit.model.{AckResult, AmqpEnvelope}
import fs2.Stream

case class SimexMessageConsumer[F[_]](
    handler: SimexMessageHandler[F],
    ackerConsumer: (AckResult => F[Unit], Stream[F, AmqpEnvelope[String]])
)
