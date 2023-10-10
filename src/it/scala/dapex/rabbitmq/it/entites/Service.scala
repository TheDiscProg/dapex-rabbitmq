package dapex.rabbitmq.it.entites

import dapex.rabbitmq.consumer.DapexMessageHandler
import dapex.rabbitmq.it.rmq.caching.CachingServiceAlgebra
import dapex.rabbitmq.publisher.DapexMQPublisher
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import dev.profunktor.fs2rabbit.model.AMQPChannel

case class Service[F[_]](
    rmqClient: RabbitClient[F],
    handlers: List[DapexMessageHandler[F]],
    rmqPublisher: DapexMQPublisher[F],
    channel: AMQPChannel,
    cachingService: CachingServiceAlgebra[F]
)
