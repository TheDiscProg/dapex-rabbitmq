package simex.rabbitmq.consumer

import cats.effect.{Concurrent, ExitCode, Temporal}
import cats.implicits._
import dev.profunktor.fs2rabbit.arguments.Arguments
import dev.profunktor.fs2rabbit.config.declaration._
import dev.profunktor.fs2rabbit.effects.Log
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import dev.profunktor.fs2rabbit.json.Fs2JsonDecoder
import dev.profunktor.fs2rabbit.model.AckResult.{Ack, NAck}
import dev.profunktor.fs2rabbit.model.{AMQPChannel, AckResult, AmqpEnvelope, DeliveryTag}
import dev.profunktor.fs2rabbit.resiliency.ResilientStream
import fs2.Stream
import io.circe.{Decoder, Error}
import org.typelevel.log4cats.Logger
import simex.messaging.Simex
import simex.rabbitmq.RabbitQueue
import simex.rabbitmq.entities.ServiceError

class SimexMQConsumer[F[_]: Concurrent: Logger](rmqClient: RabbitClient[F])(implicit
    channel: AMQPChannel
) extends SimexMQConsumerAlgebra[F] {

  private lazy val fs2JsonDecoder: Fs2JsonDecoder = new Fs2JsonDecoder

  override def consumeRMQSimexMessage(handlers: List[SimexMessageHandler[F]]): Stream[F, Unit] = {
    val consumers = setUpRMQConsumers(handlers)
    createConsumerStream(consumers)
  }

  private def createConsumerStream(consumers: F[List[SimexMessageConsumer[F]]]): Stream[F, Unit] =
    (for {
      consumer: SimexMessageConsumer[F] <- Stream.evalSeq(consumers)
      (acker, stream) = consumer.ackerConsumer
    } yield stream
      .through(decoder[Simex])
      .flatMap {
        handleDapexMessage(_)(acker)(consumer.handler.f)
      }).parJoinUnbounded

  private def setUpRMQConsumers(
      handlers: List[SimexMessageHandler[F]]
  ): F[List[SimexMessageConsumer[F]]] =
    for {
      _ <- createExchangeAndQueues(handlers.map(_.queue))
      handlers <- handlers.map { handler =>
        for {
          consumer <- getAckerConsumerForQueue(handler)
        } yield consumer
      }.sequence
    } yield handlers

  private def createExchangeAndQueues(queues: List[RabbitQueue]): F[Unit] =
    for {
      _ <- createExchanges(queues)
      _ <- createQueues(queues)
    } yield ()

  private def createExchanges(queues: List[RabbitQueue]): F[Unit] =
    queues.toVector
      .traverse(queue => createExchange(queue)) *>
      ().pure[F]

  private def createExchange(queue: RabbitQueue): F[Unit] =
    for {
      _ <- Logger[F].info(s"Creating Exchange for ${queue.exchange}")
      conf = DeclarationExchangeConfig
        .default(queue.exchange, queue.exchangeType)
        .copy(durable = Durable)
      _ <- rmqClient.declareExchange(conf)
    } yield ()

  private def createQueues(queues: List[RabbitQueue]): F[Unit] =
    queues.toVector
      .traverse(queue => createQueue(queue)) *>
      ().pure[F]

  private def createQueue(queue: RabbitQueue): F[Unit] =
    for {
      _ <- Logger[F].info(s"Creating Consumer queue [${queue.name}]")
      queueConfig = DeclarationQueueConfig(
        queueName = queue.name,
        durable = Durable,
        exclusive = NonExclusive,
        autoDelete = NonAutoDelete,
        arguments = getArgumentsForQueue(queue.dlx, queue.messageTTL)
      )
      _ <- rmqClient.declareQueue(queueConfig)
      _ <- rmqClient.bindQueue(
        queueName = queue.name,
        exchangeName = queue.exchange,
        routingKey = queue.routingKey
      )
    } yield ()

  private def getArgumentsForQueue(dlx: Option[String], messageTTL: Option[Long]): Arguments =
    (dlx, messageTTL) match {
      case (Some(dlx), Some(ttl)) =>
        Map("x-dead-letter-exchange" -> dlx, "x-message-ttl" -> ttl): Arguments
      case (Some(dlx), None) =>
        Map("x-dead-letter-exchange" -> dlx): Arguments
      case (None, Some(ttl)) =>
        Map("x-message-ttl" -> ttl): Arguments
      case _ => Map.empty: Arguments
    }

  private def getAckerConsumerForQueue(
      handler: SimexMessageHandler[F]
  ): F[SimexMessageConsumer[F]] =
    for {
      ca <- rmqClient.createAckerConsumer[String](handler.queue.name)
    } yield SimexMessageConsumer(handler = handler, ackerConsumer = ca)

  private def handleDapexMessage[E <: ServiceError](
      decodedMsg: (Either[Error, Simex], DeliveryTag)
  )(acker: AckResult => F[Unit])(f: Simex => F[Unit]): Stream[F, Unit] =
    decodedMsg match {
      case (Left(error), tag) =>
        Stream
          .eval(Logger[F].warn(error.getMessage))
          .map(_ => NAck(tag))
          .evalMap(acker)
      case (Right(msg), tag) =>
        Stream
          .eval(f(msg))
          .map(_ => Ack(tag))
          .evalMap(acker)
    }

  private def decoder[A <: Simex: Decoder]
      : Stream[F, AmqpEnvelope[String]] => Stream[F, (Either[Error, A], DeliveryTag)] =
    _.map(fs2JsonDecoder.jsonDecode[A])

}

object SimexMQConsumer {
  def consumeRMQ[F[_]: Log: Temporal: Logger](
      rmqClient: RabbitClient[F],
      handlers: List[SimexMessageHandler[F]],
      channel: AMQPChannel
  ): F[ExitCode] = {
    implicit val c = channel
    val dapexMQConsumer = new SimexMQConsumer[F](rmqClient)
    ResilientStream
      .run(dapexMQConsumer.consumeRMQSimexMessage(handlers))
      .as(ExitCode.Success)
  }

  def consumerRMQStream[F[_]: Temporal: Logger](
      rmqClient: RabbitClient[F],
      handlers: List[SimexMessageHandler[F]],
      channel: AMQPChannel
  ): Stream[F, Unit] = {
    implicit val c = channel
    val dapexMQConsumer = new SimexMQConsumer[F](rmqClient)
    dapexMQConsumer.consumeRMQSimexMessage(handlers)
  }
}
