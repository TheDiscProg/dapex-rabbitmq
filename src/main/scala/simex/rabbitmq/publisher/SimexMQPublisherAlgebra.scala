package simex.rabbitmq.publisher

import simex.messaging.Simex
import simex.rabbitmq.RabbitQueue

trait SimexMQPublisherAlgebra[F[_]] {

  def publishMessageToQueue(message: Simex, queue: RabbitQueue): F[Unit]
}
