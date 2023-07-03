package dapex.rabbitmq.consumer

import dapex.messaging.DapexMessage
import dapex.rabbitmq.RabbitQueue

case class DapexMessageHandler[F[_]](
    queue: RabbitQueue,
    f: DapexMessage => F[Unit]
)
