package com.island.ohara.kafka

import com.island.ohara.integration.With3Brokers3Workers
import com.island.ohara.io.CloseOnce
import org.junit.{After, Test}
import org.scalatest.Matchers
class TestKafkaClient extends With3Brokers3Workers with Matchers {
  import scala.concurrent.duration._
  private[this] val timeout = 10 seconds

  private[this] val client = KafkaClient(testUtil.brokers)

  @Test
  def testAddPartitions(): Unit = {
    val topicName = methodName
    KafkaUtil.createTopic(testUtil.brokers, topicName, 1, 1)
    KafkaUtil.topicInfo(testUtil.brokers, topicName, timeout).get.numberOfPartitions shouldBe 1

    KafkaUtil.addPartitions(testUtil.brokers, topicName, 2, timeout)
    KafkaUtil.topicInfo(testUtil.brokers, topicName, timeout).get.numberOfPartitions shouldBe 2

    // decrease the number
    an[IllegalArgumentException] should be thrownBy KafkaUtil.addPartitions(testUtil.brokers, topicName, 1, timeout)
    // alter an nonexistent topic
    an[IllegalArgumentException] should be thrownBy KafkaUtil.addPartitions(testUtil.brokers, "Xxx", 2, timeout)
  }

  @Test
  def testCreate(): Unit = {
    val topicName = methodName
    val numberOfPartitions = 2
    val numberOfReplications = 2.toShort
    client
      .topicCreator()
      .numberOfPartitions(numberOfPartitions)
      .numberOfReplications(numberOfReplications)
      .create(topicName)

    val topicInfo = client.topicInfo(topicName).get
    topicInfo.name shouldBe topicName
    topicInfo.numberOfPartitions shouldBe numberOfPartitions
    topicInfo.numberOfReplications shouldBe numberOfReplications

    client.deleteTopic(topicName)
    client.exist(topicName) shouldBe false
    client.topicInfo(topicName).isEmpty shouldBe true
  }

  @After
  def cleanup(): Unit = {
    CloseOnce.close(client)
  }
}
