package io.github.thediscprog.simexrabbitmq.consumer

import fs2.Stream

trait SimexMQConsumerAlgebra[F[_]] {

  def consumeRMQSimexMessage(handlers: List[SimexMessageHandler[F]]): Stream[F, Unit]
}
