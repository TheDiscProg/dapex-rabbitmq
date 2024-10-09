package io.github.thediscprog.simexrabbitmq.config

import dev.profunktor.fs2rabbit.config.Fs2RabbitConfig
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import pureconfig.{CamelCase, ConfigFieldMapping}
import pureconfig.generic.ProductHint

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

case class RabbitMQConfig(
    exchangeName: String,
    host: String,
    port: Int,
    username: String,
    password: String,
    ssl: Option[RabbitMQSSLConfig]
) {
  def asFs2RabbitConfig: Fs2RabbitConfig =
    Fs2RabbitConfig(
      host = host,
      port = port,
      virtualHost = "/",
      connectionTimeout = FiniteDuration(60, TimeUnit.SECONDS),
      ssl = ssl.isDefined,
      username = Some(username),
      password = Some(password),
      requeueOnNack = false,
      requeueOnReject = false,
      internalQueueSize = Some(1000)
    )
}

object RabbitMQConfig {
  implicit val hint: ProductHint[RabbitMQConfig] =
    ProductHint[RabbitMQConfig](ConfigFieldMapping(CamelCase, CamelCase))
  implicit val rabbitMQConfigDecoder: Decoder[RabbitMQConfig] = deriveDecoder
}
