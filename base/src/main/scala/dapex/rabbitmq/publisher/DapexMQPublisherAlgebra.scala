package dapex.rabbitmq.publisher

import dapex.messaging.DapexMessage
import dapex.rabbitmq.RabbitQueue

trait DapexMQPublisherAlgebra[F[_]] {

  def publishMessageToQueue(message: DapexMessage, queue: RabbitQueue): F[Unit]
}
