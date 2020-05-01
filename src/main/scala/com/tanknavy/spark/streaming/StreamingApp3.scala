package com.tanknavy.spark.streaming

import com.alibaba.fastjson.JSON
import com.carrotsearch.hppc.HashOrderMixing
import com.tanknavy.spark.utils.{ParamsConf, RedisPool}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, ConsumerStrategies, HasOffsetRanges, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * 版本三：Kafka消费端增加offset保存，实现语义上的exactly-once
 * data store在kafka，zookeeper, Hbase, mySQL都可以
 * https://spark.apache.org/docs/2.3.0/streaming-kafka-0-10-integration.html
 * https://blog.cloudera.com/offset-management-for-apache-kafka-with-apache-spark-streaming/
 */
object StreamingApp3 {

  def main(args: Array[String]): Unit = {

    val rootLogger = Logger.getRootLogger
    rootLogger.setLevel(Level.ERROR)

    val conf = new SparkConf().setMaster("local[2]").setAppName("StreamingApp")

    val ssc = new StreamingContext(conf,Seconds(5)) //每5秒处理一次
    //ssc.checkpoint("hdfs://kafka/offset")

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

    stream.foreachRDD(rdd => { // 对一个DStream中每一个RDD，transform是针对每个记录
      //版本三：保存offset在kafka中，万一Spark Streaming挂了不至于从头开始消费
      //val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      //版本三B: 保存offset在zookeeper中，万一Spark Streaming挂了不至于从头开始消费

      val data = rdd.map(x => JSON.parseObject(x.value())) //data的RDD中每一个元素x是json格式，
        // 版本二，优化，先过滤所需字段
        .map(x => {
          val time = x.getString("time")
          val flag = x.getString("flag") //这个订单是否已经付费
          val fee = x.getString("fee").toLong
          // tuple数据类型，两个Long
          val suceess: (Long, Long) = if (flag == "1") (1, fee) else (0, 0) //如果付费成功，就是一条有效订单+金额

          val day = time.substring(0, 8)
          val hour = time.substring(8, 10)
          val minute = time.substring(10, 12)

          //粒度(day,hour,minute)，新的数据结构tuple
          (day, hour, minute, List[Long](1, suceess._1, suceess._2)) // 订单数，有效订单，付费金额
        })

      //按天，（day,List） ->
      //data.map(x =>(x._1,x._4(1))).reduceByKey(_+_). // 方法一，测试ok
      data.map(x => (x._1, x._4)).reduceByKey((a, b) => { // 方法二，x._4是List,reduce时zip对应位置元素相加
        a.zip(b).map(x => x._1 + x._2) // list中对应元素先zip,在reduce，最终得到（总订单数，总有效订单数，总有效订单金额）
      }).foreachPartition(partition => { //一个rdd中的分区，性能更好
          val jedis = RedisPool.getJedis() // 强调：一个parition拿一个连接
          partition.foreach(x => { //特别强调，按照每个partition写入DB
            //jedis.incrBy("OrderCount-" + x._1, x._2) //键值对在redis增加值：OrderCount-20200109: 123 // 方法一，测试ok
            //哈希数据(day,(total,successOrder,successFee))
            jedis.hincrBy("OrderCount2-" + x._1, "total", x._2(0)) //键值对在redis增加值：OrderCount-20200109: 123 //方法二，hash
            jedis.hincrBy("OrderCount2-" + x._1, "success", x._2(1)) //键值对在redis增加值：OrderCount-20200109: 123 //方法二，hash
            jedis.hincrBy("OrderCount2-" + x._1, "fee", x._2(2)) //键值对在redis增加值：OrderCount-20200109: 123 //方法二，hash
          })
        })

      //按day+hour, 输入((day,time),List)为tuple
      data.map(x => ((x._1, x._2), x._4)).reduceByKey((a, b) => { // 方法二，x._4是List,reduce时zip对应位置元素相加
        a.zip(b).map(x => x._1 + x._2) // list中对应元素先zip,在reduce，最终得到（总订单数，总有效订单数，总有效订单金额）
      }).foreachPartition(partition => { //一个rdd中的分区，性能更好
        val jedis = RedisPool.getJedis() // 强调：一个parition拿一个连接
        partition.foreach(x => { //特别强调，按照每个partition写入DB
          //jedis.incrBy("OrderCount-" + x._1, x._2) //键值对在redis增加值：OrderCount-20200109: 123 // 方法一，测试ok
          //哈希数据((day,hour),(total,successOrder,successFee))
          jedis.hincrBy("OrderCount2-" + x._1._1, "total" + x._1._2, x._2(0)) //键值对在redis增加值：OrderCount-20200109: 123 //方法二，hash
          jedis.hincrBy("OrderCount2-" + x._1._1, "success" + x._1._2, x._2(1)) //键值对在redis增加值：OrderCount-20200109: 123 //方法二，hash
          jedis.hincrBy("OrderCount2-" + x._1._1, "fee" + x._1._2, x._2(2)) //键值对在redis增加值：OrderCount-20200109: 123 //方法二，hash
        })
      })

      // 版本三：spark处理完成，异步提交当前Offset到Kafka
      //stream.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)

    })
      /*
      //版本一
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

    })*/

    // 数据写入redis

    //先配置，最后启动
    ssc.start()
    ssc.awaitTermination()
  }
}
