package com.tanknavy.spark.utils

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.JedisPool

/**
 * Redis连接池
 */
object RedisPool {

  val poolConfig = new GenericObjectPoolConfig() //apache的对象连接池
  poolConfig.setMaxIdle(10)
  poolConfig.setMaxTotal(1000)

  private lazy val jedisPool = new JedisPool(poolConfig, ParamsConf.redishost)

  def getJedis() = {
    val jedis = jedisPool.getResource
    jedis.select(ParamsConf.redisDB) //redis一个数据库的连接
    jedis
  }

}
