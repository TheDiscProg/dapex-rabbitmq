package dapex.rabbitmq.it

import cats.effect.{IO, Temporal}
import cats.implicits._
import dapex.messaging._
import dapex.rabbitmq.RabbitQueue
import dapex.rabbitmq.consumer.DapexMQConsumer
import dapex.rabbitmq.it.entites.TestRabbitQueue
import dapex.rabbitmq.publisher.DapexMQPublisher
import fs2._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec._
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.utility.DockerImageName
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration._
import scala.language.reflectiveCalls

class RabbitPublisherConsumerTest extends AnyFlatSpec with Matchers with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(100, Millis))

  import cats.effect.unsafe.implicits.global

  private implicit def unsafeLogger = Slf4jLogger.getLogger[IO]

  private val emptyStream: Stream[IO, String] = Stream.empty.covary[IO]

  private val request = DapexMessage(
    endpoint = Endpoint(resource = "service.auth", method = "select"),
    client = Client(
      clientId = "app-1",
      requestId = "app-req-1",
      sourceEndpoint = "service.auth",
      authorisation = ""
    ),
    originator = Originator(
      clientId = "app-1",
      requestId = "app-req-1",
      sourceEndpoint = "auth",
      originalToken = "securitytoken"
    ),
    entity = Some(Entity("user")),
    criteria = Vector(
      Criterion("username", "test@test.com", "EQ"),
      Criterion("password", "password1234", "EQ")
    ),
    update = Vector(),
    insert = Vector(),
    process = Vector(),
    response = None
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
    val keys: IO[List[String]] = RMQService.setUpRMQService[IO]().use { service =>
      val consumerStream: fs2.Stream[IO, Unit] =
        DapexMQConsumer.consumerRMQStream(service.rmqClient, service.handlers, service.channel)

      val publisherStream: List[Stream[IO, String]] =
        TestRabbitQueue.values.toList.map(q => publishMessage(service.rmqPublisher, request, q))

      val foldedPubliserStream = publisherStream.fold(emptyStream)((s1, s2) => s1.merge(s2))

      val mergedPublisherConsumerStream: Stream[IO, Any] =
        Stream(consumerStream, foldedPubliserStream).parJoinUnbounded
          .interruptAfter(1.second)

      for {
        _ <- mergedPublisherConsumerStream.compile.drain
        keys <- service.cachingService.getAllKeys
      } yield keys
    }

    whenReady(keys.unsafeToFuture()) { ks: List[String] =>
      ks.nonEmpty shouldBe true
      ks.contains("service.auth-1") shouldBe true
      ks.contains("service.dbread-1") shouldBe true
      ks.contains("service.collectionPoint-1") shouldBe true
    }
  }

  private def publishMessage[F[_]: Temporal](
      pubisher: DapexMQPublisher[F],
      request: DapexMessage,
      queue: RabbitQueue
  ): Stream[F, String] = {
    val s = Stream
      .iterateEval("1") { i =>
        val msg = request.copy(
          client = request.client.copy(requestId = s"${queue.name.value}-$i"),
          endpoint = request.endpoint.copy(resource = queue.name.value)
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
