package dapex.rabbitmq.it.rmq.caching

import cats.Applicative
import cats.implicits._
import com.github.blemale.scaffeine.Cache
import dapex.messaging.DapexMessage

class ScaffeineCachingService[F[_]: Applicative](cache: Cache[String, DapexMessage])
    extends CachingServiceAlgebra[F] {

  override def storeInCache(key: String, value: DapexMessage): F[String] = {
    cache.put(key, value)
    key.pure[F]
  }

  override def getFromCache(key: String): F[Option[DapexMessage]] = {
    val value = cache.getIfPresent(key)
    value.pure[F]
  }

  override def getAllKeys: F[List[String]] = {
    val keys: List[String] = cache.asMap().keys.toList
    keys.pure[F]
  }
}
