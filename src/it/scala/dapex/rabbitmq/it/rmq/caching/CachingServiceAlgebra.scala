package dapex.rabbitmq.it.rmq.caching

import dapex.messaging.DapexMessage

trait CachingServiceAlgebra[F[_]] {

  def storeInCache(key: String, value: DapexMessage): F[String]

  def getFromCache(key: String): F[Option[DapexMessage]]

  def getAllKeys: F[List[String]]

}
