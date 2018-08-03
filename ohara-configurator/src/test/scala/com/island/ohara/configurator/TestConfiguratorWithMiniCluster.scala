package com.island.ohara.configurator

import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.config.{OharaConfig, OharaJson}
import com.island.ohara.configurator.kafka.KafkaClient
import com.island.ohara.configurator.store.Store
import com.island.ohara.data.OharaTopic
import com.island.ohara.integration.OharaTestUtil
import com.island.ohara.io.CloseOnce
import com.island.ohara.kafka.KafkaUtil
import com.island.ohara.rest.{BoundRestClient, ConnectorClient}
import com.island.ohara.rule.MediumTest
import com.island.ohara.serialization.{OharaDataSerializer, StringSerializer}
import org.junit.{After, Test}
import org.scalatest.Matchers

class TestConfiguratorWithMiniCluster extends MediumTest with Matchers {

  private[this] val util = OharaTestUtil.localWorkers(3, 3)
  private[this] val currentUuid = new AtomicInteger(0)
  private[this] val uuidGenerator = () => currentUuid.incrementAndGet().toString
  private[this] val configurator =
    Configurator.builder
      .uuidGenerator(uuidGenerator)
      .hostname("localhost")
      .port(0)
      .store(
        Store
          .builder(StringSerializer, OharaDataSerializer)
          .numberOfPartitions(1)
          .numberOfReplications(1)
          .topicName("TestConfiguratorWithMiniCluster")
          .brokers(util.brokersString)
          .build())
      .kafkaClient(KafkaClient(util.brokersString))
      .connectClient(ConnectorClient(util.workersString))
      .build()
  private[this] val restClient = BoundRestClient(configurator.hostname, configurator.port)
  private[this] val topicPath = s"${Configurator.VERSION}/${Configurator.TOPIC_PATH}"
  private[this] val validationPath = s"${Configurator.VERSION}/${Configurator.VALIDATION_PATH}"

  import scala.concurrent.duration._
  private[this] val timeout = 10 seconds

  @Test
  def testTopic(): Unit = {
    var topicName = methodName
    var numberOfPartitions = 2
    val numberOfReplications = 2.toShort
    var oharaTopic = OharaTopic.json(topicName, numberOfPartitions, numberOfReplications)
    var rsp = restClient.post(topicPath, oharaTopic)
    rsp.statusCode shouldBe 200
    val uuid = OharaConfig(OharaJson(rsp.body)).requireString("uuid")
    uuid shouldBe currentUuid.get().toString

    // check the information stored in kafka
    var topicInfo = KafkaUtil.topicInfo(util.brokersString, uuid, timeout).get
    topicInfo.name shouldBe uuid
    topicInfo.numberOfPartitions shouldBe numberOfPartitions
    topicInfo.numberOfReplications shouldBe numberOfReplications

    // check the information stored in ohara
    var storedTopic: OharaTopic = configurator.store.iterator.next()._2.asInstanceOf[OharaTopic]
    storedTopic.uuid shouldBe uuid
    storedTopic.name shouldBe topicName
    storedTopic.numberOfPartitions shouldBe numberOfPartitions
    storedTopic.numberOfReplications shouldBe numberOfReplications

    numberOfPartitions = 3
    topicName = s"${methodName}123"
    oharaTopic = OharaTopic.json(topicName, numberOfPartitions, numberOfReplications)
    rsp = restClient.put(s"$topicPath/$uuid", oharaTopic)
    rsp.statusCode shouldBe 200
    OharaConfig(OharaJson(rsp.body)).requireString("uuid") shouldBe currentUuid.get().toString

    // check the information stored in kafka
    topicInfo = KafkaUtil.topicInfo(util.brokersString, uuid, timeout).get

    topicInfo.name shouldBe uuid
    topicInfo.numberOfPartitions shouldBe numberOfPartitions
    topicInfo.numberOfReplications shouldBe numberOfReplications

    // check the information stored in ohara
    storedTopic = configurator.store.iterator.next()._2.asInstanceOf[OharaTopic]
    storedTopic.uuid shouldBe uuid
    storedTopic.name shouldBe topicName
    storedTopic.numberOfPartitions shouldBe numberOfPartitions
    storedTopic.numberOfReplications shouldBe numberOfReplications

    rsp = restClient.get(s"$topicPath/$uuid")
    rsp.statusCode shouldBe 200
    val rval = OharaTopic.apply(OharaJson(rsp.body))
    rval.uuid shouldBe uuid
    rval.name shouldBe topicName
    rval.numberOfPartitions shouldBe numberOfPartitions
    rval.numberOfReplications shouldBe numberOfReplications

    // the topic should in kafka cluster
    KafkaUtil.exist(util.brokersString, uuid, timeout) shouldBe true
    rsp = restClient.delete(s"$topicPath/$uuid")
    rsp.statusCode shouldBe 200
    // the topic should be gone
    KafkaUtil.exist(util.brokersString, uuid, timeout) shouldBe false
    // the meta data should be removed from configurator too
    configurator.store.iterator.map(_._1).filter(_ == uuid).size shouldBe 0
  }

  @Test
  def testValidation(): Unit = {
    val rsp = restClient.put(validationPath, OharaJson("{\"target\":\"hdfs\",\"url\":\"file:///tmp\"}"), 30 seconds)
    rsp.statusCode shouldBe 200
    rsp.body.contains("\"status\":\"pass\"") shouldBe true
    rsp.body.contains("\"status\":\"failed\"") shouldBe false
  }

  @After
  def tearDown(): Unit = {
    CloseOnce.close(restClient)
    CloseOnce.close(configurator)
    util.close()
  }
}
