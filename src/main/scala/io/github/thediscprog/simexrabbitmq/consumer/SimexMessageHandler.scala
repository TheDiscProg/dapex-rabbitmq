package io.github.thediscprog.simexrabbitmq.consumer

import io.github.thediscprog.simexmessaging.messaging.Simex
import io.github.thediscprog.simexrabbitmq.RabbitQueue

case class SimexMessageHandler[F[_]](
    queue: RabbitQueue,
    f: Simex => F[Unit]
)
