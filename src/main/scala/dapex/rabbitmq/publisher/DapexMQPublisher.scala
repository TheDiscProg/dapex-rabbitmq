package dapex.rabbitmq.publisher

import cats.Applicative
import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import dapex.messaging.DapexMessage
import dapex.rabbitmq.RabbitQueue
import dev.profunktor.fs2rabbit.effects.MessageEncoder
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import dev.profunktor.fs2rabbit.json.Fs2JsonEncoder
import dev.profunktor.fs2rabbit.model.AmqpMessage
import io.circe.Encoder
import org.typelevel.log4cats.Logger

class DapexMQPublisher[F[_]: Sync: Logger](rabbitClient: RabbitClient[F])
    extends DapexMQPublisherAlgebra[F] {
  import DapexMQPublisher._

  def publishMessageToQueue(message: DapexMessage, queue: RabbitQueue): F[Unit] =
    for {
      _ <- Logger[F].info(
        s"Publishing DAPEX message to ${queue.name}, Message Request: ${message.client.requestId}"
      )
      response <- rabbitClient.createConnectionChannel
        .use { implicit channel =>
          rabbitClient
            .createPublisher(
              queue.exchange,
              queue.routingKey
            )(channel, encoder[F, DapexMessage])
            .flatMap { f =>
              f(message)
            }
        }
    } yield response
}

object DapexMQPublisher {
  object ioEncoder extends Fs2JsonEncoder

  implicit def encoder[F[_]: Applicative, A](implicit enc: Encoder[A]): MessageEncoder[F, A] =
    Kleisli { (a: A) =>
      val message = enc(a).noSpaces
      AmqpMessage.stringEncoder[F].run(message)
    }
}
