package dapex.rabbitmq.consumer

import fs2.Stream

trait DapexMQConsumerAlgebra[F[_]] {

  def consumeRMQDapexMessage(handlers: List[DapexMessageHandler[F]]): Stream[F, Unit]
}
