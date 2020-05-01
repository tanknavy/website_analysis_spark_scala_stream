package com.tanknavy.spark.utils

import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.StringDeserializer

/**
 *
 * 在spark_www开发中使用java.uti.Properties来配置
 * 这里使用com.typesafe的config来读取配置
 */

object ParamsConf {
  private lazy val config = ConfigFactory.load()

  val topic = config.getString("kafka.topic")
  val redisDB = config.getInt("redis.db")
  val redishost = config.getString("redis.host")
  val brokers = config.getString("kafka.broker.list")
  val group = config.getString("kafka.group.id")

  // kafka消费配置参数
  val kafkaParams = Map[String,Object](
    "bootstrap.servers" -> brokers,
    "key.deserializer" -> classOf[StringDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> group,
    "auto.offset.reset" -> "latest", //读取消息队列里面最新的
    "enable.auto.commit" -> "false" //
  )


  def main(args: Array[String]): Unit = {
    println(ParamsConf.topic)
    println(ParamsConf.redisDB)
    println(kafkaParams.toString())
  }


}
