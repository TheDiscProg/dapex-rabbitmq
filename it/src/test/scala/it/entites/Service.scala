package it.entites

import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import dev.profunktor.fs2rabbit.model.AMQPChannel
import it.rmq.caching.CachingServiceAlgebra
import simex.rabbitmq.consumer.SimexMessageHandler
import simex.rabbitmq.publisher.SimexMQPublisher

case class Service[F[_]](
    rmqClient: RabbitClient[F],
    handlers: List[SimexMessageHandler[F]],
    rmqPublisher: SimexMQPublisher[F],
    channel: AMQPChannel,
    cachingService: CachingServiceAlgebra[F]
)
