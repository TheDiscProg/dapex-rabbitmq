package io.github.thediscprog.simexrabbitmq

import dev.profunktor.fs2rabbit.model.{ExchangeName, ExchangeType, QueueName, RoutingKey}

trait RabbitQueue {
  val name: QueueName
  val routingKey: RoutingKey
  val exchange: ExchangeName
  val dlx: Option[String]
  val exchangeType: ExchangeType
  val messageTTL: Option[Long]
  val consumers: Boolean
}
