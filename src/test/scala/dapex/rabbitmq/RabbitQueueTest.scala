package dapex.rabbitmq

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RabbitQueueTest extends AnyFlatSpec with Matchers {

  it should "find service.auth" in {
    val result = RabbitQueue.withName("service.auth")

    result shouldBe RabbitQueue.SERVICE_AUTHENTICATION_QUEUE
  }

  it should "find service.dbread" in {
    val result = RabbitQueue.withName("service.dbread")

    result shouldBe RabbitQueue.SERVICE_DBREAD_QUEUE
  }

  it should "find service.collectionPoint" in {
    val result = RabbitQueue.withName("service.collectionPoint")

    result shouldBe RabbitQueue.SERVICE_COLLECTION_POINT_QUEUE
  }

  it should "throw exception" in {
    assertThrows[NoSuchElementException] {
      RabbitQueue.withName("service.unsupported")
    }
  }
}
