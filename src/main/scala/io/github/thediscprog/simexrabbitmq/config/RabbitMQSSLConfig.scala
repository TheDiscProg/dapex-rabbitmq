package io.github.thediscprog.simexrabbitmq.config

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import pureconfig.{CamelCase, ConfigFieldMapping}
import pureconfig.generic.ProductHint

case class RabbitMQSSLConfig(
    keyPassPhrase: String,
    trustPassPhrase: String,
    keyCertPath: String,
    trustStorePath: String
)

object RabbitMQSSLConfig {
  implicit val hint: ProductHint[RabbitMQSSLConfig] =
    ProductHint[RabbitMQSSLConfig](ConfigFieldMapping(CamelCase, CamelCase))
  implicit val rabbitSslConfig: Decoder[RabbitMQSSLConfig] = deriveDecoder
}
