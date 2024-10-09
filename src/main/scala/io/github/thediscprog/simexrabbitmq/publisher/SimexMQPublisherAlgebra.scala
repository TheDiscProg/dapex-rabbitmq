package io.github.thediscprog.simexrabbitmq.publisher

import io.github.thediscprog.simexmessaging.messaging.Simex
import io.github.thediscprog.simexrabbitmq.RabbitQueue

trait SimexMQPublisherAlgebra[F[_]] {

  def publishMessageToQueue(message: Simex, queue: RabbitQueue): F[Unit]
}
