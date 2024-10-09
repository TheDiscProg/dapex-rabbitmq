package io.github.thediscprog.simexrabbitmq.publisher

import cats.Applicative
import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import dev.profunktor.fs2rabbit.effects.MessageEncoder
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import dev.profunktor.fs2rabbit.json.Fs2JsonEncoder
import dev.profunktor.fs2rabbit.model
import dev.profunktor.fs2rabbit.model.AmqpMessage
import io.circe.Encoder
import io.github.thediscprog.simexmessaging.messaging.Simex
import io.github.thediscprog.simexrabbitmq.RabbitQueue
import org.typelevel.log4cats.Logger

class SimexMQPublisher[F[_]: Sync: Logger](rabbitClient: RabbitClient[F])
    extends SimexMQPublisherAlgebra[F] {
  import SimexMQPublisher._

  def publishMessageToQueue(message: Simex, queue: RabbitQueue): F[Unit] =
    for {
      _ <- Logger[F].info(
        s"Publishing DAPEX message to ${queue.name}, Message Request: ${message.client.requestId}"
      )
      response <- rabbitClient.createConnectionChannel
        .use { implicit channel: model.AMQPChannel =>
          rabbitClient
            .createPublisher(
              queue.exchange,
              queue.routingKey
            )(channel, encoder[F, Simex])
            .flatMap { f =>
              f(message)
            }
        }
    } yield response
}

object SimexMQPublisher {
  object ioEncoder extends Fs2JsonEncoder

  implicit def encoder[F[_]: Applicative, A](implicit enc: Encoder[A]): MessageEncoder[F, A] =
    Kleisli { (a: A) =>
      val message = enc(a).noSpaces
      AmqpMessage.stringEncoder[F].run(message)
    }
}
