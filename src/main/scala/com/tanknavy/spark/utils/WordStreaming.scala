package com.tanknavy.spark.utils

import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.util.LongAccumulator

object WordBlacklist {
  @volatile private var blacklist:Broadcast[Seq[String]] = null;

  def getInstance(sc: SparkContext):Broadcast[Seq[String]] = {
    if (blacklist == null){
      synchronized {
        if (blacklist == null){
          val list = Seq("a","b","c")
          blacklist = sc.broadcast(list)
        }
      }
    }
    blacklist
  }
}

object WordBlacklistCounter{
  @volatile private var blacklistCounter:LongAccumulator = null;
  def getInstance(sc: SparkContext) :LongAccumulator = {
    if (blacklistCounter == null){
      synchronized{
        if(blacklistCounter == null){
          blacklistCounter = sc.longAccumulator("wordsBlacklistCounter")
        }
      }
    }
    blacklistCounter
  }
}

