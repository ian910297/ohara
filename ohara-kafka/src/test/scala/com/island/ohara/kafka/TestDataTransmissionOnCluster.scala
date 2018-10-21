package com.island.ohara.kafka

import com.island.ohara.client.ConfiguratorJson.Column
import com.island.ohara.client.ConnectorJson.State
import com.island.ohara.data.{Cell, Row}
import com.island.ohara.integration.{OharaTestUtil, With3Brokers3Workers}
import com.island.ohara.io.CloseOnce._
import com.island.ohara.io.{ByteUtil, CloseOnce}
import com.island.ohara.kafka.connector._
import com.island.ohara.serialization.{DataType, RowSerializer}
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class TestDataTransmissionOnCluster extends With3Brokers3Workers with Matchers {

  private[this] val kafkaClient = KafkaClient(testUtil.brokersConnProps)
  private[this] val row = Row(Cell("cf0", 10), Cell("cf1", 11))
  private[this] val schema = Seq(Column("cf", DataType.BOOLEAN, 1))
  private[this] val numberOfRows = 20

  @After
  def tearDown(): Unit = CloseOnce.close(kafkaClient)

  private[this] def createTopic(topicName: String, compacted: Boolean): Unit = {
    if (compacted)
      kafkaClient.topicCreator().compacted().numberOfPartitions(1).numberOfReplications(1).create(topicName)
    else
      kafkaClient.topicCreator().deleted().numberOfPartitions(1).numberOfReplications(1).create(topicName)
  }

  private[this] def setupData(topicName: String): Unit = {
    doClose(Producer.builder().brokers(testUtil.brokersConnProps).build[Array[Byte], Row]) { producer =>
      0 until numberOfRows foreach (_ => producer.sender().key(ByteUtil.toBytes("key")).value(row).send(topicName))
    }
    checkData(topicName)
  }

  private[this] def checkData(topicName: String): Unit = doClose(
    Consumer
      .builder()
      .offsetFromBegin()
      .brokers(testUtil.brokersConnProps)
      .topicName(topicName)
      .build[Array[Byte], Row]) { consumer =>
    val data = consumer.poll(30 seconds, numberOfRows)
    data.size shouldBe numberOfRows
    data.foreach(_.value.get shouldBe row)
  }

  private[this] def checkConnector(name: String): Unit = {
    OharaTestUtil.await(() => testUtil.connectorClient.activeConnectors().contains(name), 30 seconds)
    OharaTestUtil.await(() => testUtil.connectorClient.config(name).topics.nonEmpty, 30 seconds)
    OharaTestUtil.await(() =>
                          try testUtil.connectorClient.status(name).connector.state == State.RUNNING
                          catch {
                            case _: Throwable => false
                        },
                        30 second)
  }

  @Test
  def testRowProducer2RowConsumer(): Unit = {
    var topicName = methodName
    //test deleted topic
    createTopic(topicName, false)
    testRowProducer2RowConsumer(topicName)

    topicName = methodName + "-2"
    //test compacted topic
    createTopic(topicName, true)
    testRowProducer2RowConsumer(topicName)
  }

  /**
    * producer -> topic_1(topicName) -> consumer
    */
  private[this] def testRowProducer2RowConsumer(topicName: String): Unit = {
    setupData(topicName)
    doClose(
      Consumer
        .builder()
        .brokers(testUtil.brokersConnProps)
        .offsetFromBegin()
        .topicName(topicName)
        .build[Array[Byte], Row]) { consumer =>
      val data = consumer.poll(10 seconds, numberOfRows)
      data.size shouldBe numberOfRows
      data.foreach(r => r.value.get shouldBe row)
    }
  }

  @Test
  def testProducer2SinkConnector(): Unit = {
    var topicName = methodName
    var topicName2 = methodName + "-2"
    //test deleted topic
    createTopic(topicName, false)
    createTopic(topicName2, false)
    testProducer2SinkConnector(topicName, topicName2)

    topicName = methodName + "-3"
    topicName2 = methodName + "-4"
    //test compacted topic
    createTopic(topicName, true)
    createTopic(topicName2, true)
    testProducer2SinkConnector(topicName, topicName2)
  }

  /**
    * producer -> topic_1(topicName) -> sink connector -> topic_2(topicName2)
    */
  private[this] def testProducer2SinkConnector(topicName: String, topicName2: String): Unit = {
    val connectorName = methodName
    testUtil.connectorClient
      .connectorCreator()
      .name(connectorName)
      .connectorClass(classOf[SimpleRowSinkConnector])
      .topic(topicName)
      .numberOfTasks(2)
      .disableConverter()
      .schema(schema)
      .configs(Map(Constants.BROKER -> testUtil.brokersConnProps, Constants.OUTPUT -> topicName2))
      .create()

    try {
      checkConnector(connectorName)
      setupData(topicName)
      checkData(topicName2)
    } finally testUtil.connectorClient.delete(connectorName)
  }

  @Test
  def testSourceConnector2Consumer(): Unit = {
    var topicName = methodName
    var topicName2 = methodName + "-2"
    //test deleted topic
    createTopic(topicName, false)
    createTopic(topicName2, false)
    testSourceConnector2Consumer(topicName, topicName2)

    topicName = methodName + "-3"
    topicName2 = methodName + "-4"
    //test compacted topic
    createTopic(topicName, true)
    createTopic(topicName2, true)
    testSourceConnector2Consumer(topicName, topicName2)
  }

  /**
    * producer -> topic_1(topicName) -> row source -> topic_2 -> consumer
    */
  private[this] def testSourceConnector2Consumer(topicName: String, topicName2: String): Unit = {
    val connectorName = methodName
    testUtil.connectorClient
      .connectorCreator()
      .name(connectorName)
      .connectorClass(classOf[SimpleRowSourceConnector])
      .topic(topicName2)
      .numberOfTasks(2)
      .disableConverter()
      .schema(schema)
      .configs(Map(Constants.BROKER -> testUtil.brokersConnProps, Constants.INPUT -> topicName))
      .create()

    try {
      checkConnector(connectorName)
      setupData(topicName)
      checkData(topicName2)
    } finally testUtil.connectorClient.delete(connectorName)
  }

  /**
    * Test case for OHARA-150
    */
  @Test
  def shouldKeepColumnOrderAfterSendToKafka(): Unit = {
    val topicName = methodName
    KafkaUtil.createTopic(testUtil.brokersConnProps, topicName, 1, 1)

    val row = Row(Cell("c", 3), Cell("b", 2), Cell("a", 1))
    doClose(Producer.builder().brokers(testUtil.brokersConnProps).build[String, Row]) { producer =>
      producer.sender().key(topicName).value(row).send(topicName)
      producer.flush()
    }

    val consumer =
      Consumer.builder().brokers(testUtil.brokersConnProps).offsetFromBegin().topicName(topicName).build[String, Row]

    try {
      val fromKafka = consumer.poll(30 seconds, 1)
      fromKafka.isEmpty shouldBe false
      val row = fromKafka.head.value.get
      row.cell(0).name shouldBe "c"
      row.cell(1).name shouldBe "b"
      row.cell(2).name shouldBe "a"

    } finally consumer.close()

    doClose(Producer.builder().brokers(testUtil.brokersConnProps).build[String, Row]) { producer =>
      val meta = Await.result(producer.sender().key(topicName).value(row).send(topicName), 10 seconds)
      meta.topic shouldBe topicName
    }
  }

  /**
    * Test for ConnectorClient
    * @see ConnectorClient
    */
  @Test
  def connectorClientTest(): Unit = {
    val connectorName = "connectorClientTest"
    val topics = Seq("connectorClientTest_topic", "connectorClientTest_topic2")
    val output_topic = "connectorClientTest_topic_output"
    val connectorClient = testUtil.connectorClient
    connectorClient
      .connectorCreator()
      .name(connectorName)
      .connectorClass(classOf[SimpleRowSinkConnector])
      .topics(topics)
      .numberOfTasks(2)
      .disableConverter()
      .schema(schema)
      .configs(Map(Constants.BROKER -> testUtil.brokersConnProps, Constants.OUTPUT -> output_topic))
      .create()

    val activeConnectors = connectorClient.activeConnectors()
    activeConnectors.contains(connectorName) shouldBe true

    val config = connectorClient.config(connectorName)
    config.topics shouldBe topics

    OharaTestUtil.await(() => connectorClient.status(connectorName).tasks != Nil, 10 second)
    var status = connectorClient.status(connectorName)
    status.tasks.head should not be null

    val task = connectorClient.taskStatus(connectorName, status.tasks.head.id)
    task should not be null
    task == status.tasks.head shouldBe true
    task.worker_id.isEmpty shouldBe false

    connectorClient.delete(connectorName)
    connectorClient.activeConnectors().contains(connectorName) shouldBe false
  }
}
