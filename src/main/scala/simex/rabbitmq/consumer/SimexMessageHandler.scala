package simex.rabbitmq.consumer

import simex.messaging.Simex
import simex.rabbitmq.RabbitQueue

case class SimexMessageHandler[F[_]](
    queue: RabbitQueue,
    f: Simex => F[Unit]
)
