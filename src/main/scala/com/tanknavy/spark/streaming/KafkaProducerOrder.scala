package com.tanknavy.spark.streaming

import java.util
import java.util.{Date, Properties, UUID}

import com.alibaba.fastjson.JSONObject
import com.tanknavy.spark.utils.ParamsConf
import org.apache.commons.lang3.time.FastDateFormat
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.util.Random

object KafkaProducerOrder {
  def main(args: Array[String]): Unit = {

    val prop = new Properties()
    prop.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer") //这里要写全路径
    prop.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer")
    prop.put("bootstrap.servers",ParamsConf.brokers)
    prop.put("request.required.acks","1")
    val topic = ParamsConf.topic

    val producer = new KafkaProducer[String,String](prop)

    val random = new Random()
    val dateFormat = FastDateFormat.getInstance("yyyyMMddHHmmss")

    for(i <- 0 until 100){

      val time = dateFormat.format(new Date())
      val userid = random.nextInt(1000).toString
      val courseid = random.nextInt(500).toString
      val fee = random.nextInt(400).toString
      val result = Array("0","1") //是否付钱
      val flag = result(random.nextInt(2)) //选择0,1
      val orderid = UUID.randomUUID().toString

      //json格式
      val map = new util.HashMap[String, Object]() //java.util.HashMap, 注意scala语法，泛型用[],java中用<>
      map.put("time",time)
      map.put("userid",userid)
      map.put("courseid",courseid)
      map.put("fee",fee)
      map.put("flag",flag)
      map.put("orderid",orderid)

      val json = new JSONObject(map)

      producer.send(new ProducerRecord[String,String](topic, i+"", json.toString)) //[k/v](topic,key,value)
    }

    println("Kafka producer data")
  }
}
