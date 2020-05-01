Spark Streaming实时处理

项目架构及处理流程

批量：HDFS=>Spark=>Hbase=>Spark=>Mysql=>UI
实时：Log=>Flume=>Kafla=>SparkStreaming(Direct/Receiver)=>Redis=>UI

大数据团队分工：采集，批处理，实时处理，API，前端

项目需求
1)统计每天付费成功的总订单数，订单总金额
2)统计每小时付费成功的总订单数，订单总金额
3)统计每分钟付费成功的总订单数，订单总金额
4)基于Window付费成功的总订单数，订单金额
5)付费订单占总下单的占比：天，小时，分钟

SparkStreaming读取Kafka的数据，通过fastjson的方式把所需的字段解析出来
Kafka消费参数： "auto.offset.reset" -> "latest"

Spark Streaming工作流程：
StreamingContext
从Kafka中获取要处理的数据
根据业务来处理数据
处理结果入库
启动程序，等待程序终止

https://spark.apache.org/docs/2.3.0/streaming-kafka-0-10-integration.html
如果业务处理系统关了，kafka数据到底是从头开始还是从最新数据开始？
正确做法：
    第一次应用程序启动的时候，应该从某个地方获取已经消费过的offset
    业务逻辑处理完之后，应该要把已经处理完的offset给它保存到某个地方去
KafkaUtils.createDirectStream

offset储存的地方：
    Checkpoint: 生产环境不能使用
    Kafka
    MySQL,zookeeper