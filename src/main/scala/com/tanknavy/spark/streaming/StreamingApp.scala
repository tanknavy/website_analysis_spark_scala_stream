package com.tanknavy.spark.streaming

import com.alibaba.fastjson.JSON
import com.tanknavy.spark.utils.{ParamsConf, RedisPool}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.log4j.{Level, Logger}

object StreamingApp {

  def main(args: Array[String]): Unit = {

    val rootLogger = Logger.getRootLogger
    rootLogger.setLevel(Level.ERROR)

    val conf = new SparkConf().setMaster("local[2]").setAppName("StreamingApp")

    val ssc = new StreamingContext(conf,Seconds(5)) //每5秒处理一次


    val stream = KafkaUtils.createDirectStream(ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String,String](Array(ParamsConf.topic).toSet, ParamsConf.kafkaParams) //需要topics，
    )
    //测试
    //stream.map(x => x.value()).print() //默认只打印前10条
    /**
     * 统计每天付费成功的总订单数
     * 统计每天付费陈宫的总订单金额
     */
    stream.foreachRDD(rdd =>{ // 对一个DStream中每一个RDD，transform是针对每个记录
      val data = rdd.map(x => JSON.parseObject(x.value())) //RDD中每一个元素x，
      data.cache()
      data.map(x =>{
        val time = x.getString("time")
        val day = time.substring(0,8)
        val flag = x.getString("flag") //这个订单是否已经付费
        val flagResult = if(flag == "1") 1 else 0

        (day,flagResult) //(日期，是否付费)，记得rdd中总是返回一个tuple
      }).reduceByKey(_+_).
        //collect().foreach(println) //测试用
      foreachPartition(partition => { //一个rdd中的分区，性能更好
        val jedis = RedisPool.getJedis() // 强调：一个parition拿一个连接
        partition.foreach(x =>{ //特别强调，按照每个partition写入DB
          jedis.incrBy("OrderCount-" + x._1, x._2) //键值对在redis增加值：OrderCount-20200109: 123
        })
      })

      //每天付费总订单金额
      data.map(x =>{
        val time = x.getString("time")
        val day = time.substring(0,8)
        val fee = x.getString("fee").toLong
        val flag = x.getString("flag") //这个订单是否已经付费
        val flagFee = if(flag == "1") fee else 0

        (day,flagFee) //(日期，是否付费)，记得rdd中总是返回一个tuple
      }).reduceByKey(_+_).collect().foreach(println) //

      data.unpersist(true)
    })

    // 数据写入redis

    //先配置，最后启动
    ssc.start()
    ssc.awaitTermination()
  }
}
