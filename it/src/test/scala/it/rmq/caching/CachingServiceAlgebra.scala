package it.rmq.caching

import simex.messaging.Simex

trait CachingServiceAlgebra[F[_]] {

  def storeInCache(key: String, value: Simex): F[String]

  def getFromCache(key: String): F[Option[Simex]]

  def getAllKeys: F[List[String]]

}
