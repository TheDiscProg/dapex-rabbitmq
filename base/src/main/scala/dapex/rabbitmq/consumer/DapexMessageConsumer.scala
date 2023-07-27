package dapex.rabbitmq.consumer

import dev.profunktor.fs2rabbit.model.{AckResult, AmqpEnvelope}
import fs2.Stream

case class DapexMessageConsumer[F[_]](
    handler: DapexMessageHandler[F],
    ackerConsumer: (AckResult => F[Unit], Stream[F, AmqpEnvelope[String]])
)
