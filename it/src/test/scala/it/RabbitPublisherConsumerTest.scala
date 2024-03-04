package it

import cats.effect.{IO, Temporal}
import cats.implicits._
import fs2._
import it.entites.TestRabbitQueue
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec._
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.utility.DockerImageName
import org.typelevel.log4cats.slf4j.Slf4jLogger
import simex.messaging.{Datum, Simex}
import simex.rabbitmq.RabbitQueue
import simex.rabbitmq.consumer.SimexMQConsumer
import simex.rabbitmq.publisher.SimexMQPublisher
import simex.test.SimexTestFixture
import thediscprog.slogic.Xor

import scala.concurrent.duration._
import scala.language.reflectiveCalls

class RabbitPublisherConsumerTest
    extends AnyFlatSpec
    with Matchers
    with ScalaFutures
    with OptionValues
    with SimexTestFixture {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(100, Millis))

  import cats.effect.unsafe.implicits.global

  private implicit def unsafeLogger = Slf4jLogger.getLogger[IO]

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
    println(s"Ports listening: ${container.getLivenessCheckPortNumbers}")
    container.getLivenessCheckPortNumbers.contains(5672) shouldBe true
    container.getAdminPassword shouldBe "adminpassword"
    container.getAdminUsername shouldBe "guest"
  }

  it should "publish to and receive from RMQ" in {
    val keysAndValue: IO[(List[String], Option[Simex])] =
      RMQService.setUpRMQService[IO]().use { service =>
        val consumerStream: fs2.Stream[IO, Unit] =
          SimexMQConsumer.consumerRMQStream(service.rmqClient, service.handlers, service.channel)

        val publisherStream: List[Stream[IO, String]] =
          TestRabbitQueue.values.toList.map(q => publishMessage(service.rmqPublisher, request, q))

        val foldedPubliserStream = publisherStream.fold(emptyStream)((s1, s2) => s1.merge(s2))

        val mergedPublisherConsumerStream: Stream[IO, Any] =
          Stream(consumerStream, foldedPubliserStream).parJoinUnbounded
            .interruptAfter(1.second)

        for {
          _ <- mergedPublisherConsumerStream.compile.drain
          keys <- service.cachingService.getAllKeys
          value <- service.cachingService.getFromCache("service.auth-1")
        } yield (keys, value)
      }

    whenReady(keysAndValue.unsafeToFuture()) { ks: (List[String], Option[Simex]) =>
      val keys = ks._1
      val value = ks._2
      keys.nonEmpty shouldBe true
      keys.contains("service.auth-1") shouldBe true
      keys.contains("service.dbread-1") shouldBe true
      keys.contains("service.collectionPoint-10") shouldBe true
      value.isDefined shouldBe true
      value.value.data shouldBe request.data
    }
  }

  private def publishMessage[F[_]: Temporal](
      pubisher: SimexMQPublisher[F],
      request: Simex,
      queue: RabbitQueue
  ): Stream[F, String] = {
    val s = Stream
      .iterateEval("1") { i =>
        val msg = request.copy(
          client = request.client.copy(requestId = s"${queue.name.value}-$i"),
          destination = request.destination.copy(resource = queue.name.value)
        )
        pubisher.publishMessageToQueue(msg, queue) *> ((i.toInt + 1).toString)
          .pure[F]
      }
    s
  }

  private def setUpRabbitMQ(): RabbitMQContainer = {
    val dockerImage = DockerImageName.parse("rabbitmq:management")
    val container = new RabbitMQContainer(dockerImage) {
      def addFixedPort(hostPort: Int, containerPort: Int): Unit =
        super.addFixedExposedPort(hostPort, containerPort)
    }
    container.withAdminPassword("adminpassword")
    container.addFixedPort(5672, 5672)
    container.start()
    container
  }
}
