package io.github.thediscprog.simexrabbitmq.it

import cats.effect.{IO, Resource, Temporal}
import cats.implicits._
import fs2._
import io.github.thediscprog.simexmessaging.messaging.{Datum, Simex}
import io.github.thediscprog.simexmessaging.test.SimexTestFixture
import io.github.thediscprog.simexrabbitmq.consumer.SimexMQConsumer
import io.github.thediscprog.simexrabbitmq.it.entites.TestRabbitQueue
import io.github.thediscprog.simexrabbitmq.publisher.SimexMQPublisher
import io.github.thediscprog.simexrabbitmq.{Rabbit, RabbitQueue}
import io.github.thediscprog.slogic.Xor
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec._
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.utility.DockerImageName
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.reflectiveCalls

class RabbitPublisherConsumerTest
    extends AnyFlatSpec
    with Matchers
    with ScalaFutures
    with OptionValues
    with SimexTestFixture {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(500, Millis))

  import cats.effect.unsafe.implicits.global

  private implicit def unsafeLogger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  private val emptyStream: Stream[IO, String] = Stream.empty.covary[IO]

  // Make sure that it can handle recursive Xor values
  private val request = authenticationRequest.copy(
    data = authenticationRequest.data ++ Vector(
      Datum(
        "person",
        None,
        Xor.applyRight(
          Vector(
            Datum("name", None, Xor.applyLeft("John Smith")),
            Datum("title", None, Xor.applyLeft("Mr")),
            Datum(
              "address",
              None,
              Xor.applyRight(
                Vector(
                  Datum("street", None, Xor.applyLeft("The Street")),
                  Datum("postcode", None, Xor.applyLeft("AA1 1AA"))
                )
              )
            )
          )
        )
      )
    )
  )
  val container = setUpRabbitMQ()

  it should "start the RMQ" in {
    container.isRunning shouldBe true
    container.getAdminPassword shouldBe "adminpassword"
    container.getAdminUsername shouldBe "guest"
  }

  it should "publish to and receive from RMQ" in {
    val rmqMappedPort = container.getMappedPort(5672)
    val keysAndValue: Future[(List[String], Option[Simex])] =
      RMQService
        .setUpRMQService[IO]()
        .use { service =>
          Resource
            .eval(
              Rabbit
                .getRabbitClient(service.rmqConf.copy(port = rmqMappedPort), service.rmqDispatcher)
            )
            .use { rmqClient =>
              rmqClient.createConnectionChannel.use { channel =>
                val rmqPublisher = new SimexMQPublisher(rmqClient)
                val publisherStream: List[Stream[IO, String]] =
                  TestRabbitQueue.values.toList
                    .map(q => publishMessage(rmqPublisher, request, q))

                val foldedPublisherStream: Stream[IO, String] =
                  publisherStream.fold(emptyStream)((s1, s2) => s1.merge(s2))

                val consumerStream: fs2.Stream[IO, Unit] =
                  SimexMQConsumer
                    .consumerRMQStream(rmqClient, service.handlers, channel)
                val mergedStream = Stream(consumerStream, foldedPublisherStream).parJoinUnbounded
                  .interruptAfter(1.second)

                for {
                  _ <- mergedStream.compile.drain
                  keys <- service.cachingService.getAllKeys
                  value <- service.cachingService.getFromCache("service.auth-2")
                } yield (keys, value)
              }
            }
        }
        .unsafeToFuture()

    whenReady(keysAndValue) { ks =>
      val keys: List[String] = ks._1
      val value: Option[Simex] = ks._2
      keys.nonEmpty shouldBe true
      // We are testing the last keys as the first sometimes is dropped
      // the RabbitMQ queues are being set up
      keys.contains("service.auth-5") shouldBe true
      keys.contains("service.dbread-5") shouldBe true
      keys.contains("service.collectionPoint-5") shouldBe true
      value.isDefined shouldBe true
      value.value.data shouldBe request.data
    }
  }

  private def publishMessage[F[_]: Temporal](
      publisher: SimexMQPublisher[F],
      request: Simex,
      queue: RabbitQueue
  ): Stream[F, String] = {
    val s: Stream[F, String] = Stream.evalSeq(Seq(0, 1, 2, 3, 4, 5).pure[F]).evalMap { i =>
      val msg = request.copy(
        client = request.client.copy(requestId = s"${queue.name.value}-$i"),
        destination = request.destination.copy(resource = queue.name.value)
      )
      publisher.publishMessageToQueue(msg, queue) *> i.toString.pure[F]
    }
    s
  }

  private def setUpRabbitMQ(): RabbitMQContainer = {
    val dockerImage = DockerImageName.parse("rabbitmq:management")
    val container = new RabbitMQContainer(dockerImage)
      .withAdminPassword("adminpassword")
    container.start()
    container
  }
}
