package com.tanknavy.spark.utils

import java.util
import java.util.{Date, UUID}

import com.alibaba.fastjson.JSONObject
import org.apache.commons.lang3.time.FastDateFormat

import scala.util.Random


/**
 * 付费日志产生器
 */
object MockData {
  def main(args: Array[String]): Unit = {
    val random = new Random()
    val dateFormat = FastDateFormat.getInstance("yyyyMMddHHmmss")

    // time,userid,courseid,orderid,fee,flag
    for(i <- 0 to 9){
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

      println(json)
    }
  }
}
