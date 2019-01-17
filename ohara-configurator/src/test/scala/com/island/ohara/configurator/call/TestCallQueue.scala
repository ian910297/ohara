/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.configurator.call

import java.time.Duration
import java.util.concurrent.{TimeUnit, TimeoutException}

import com.island.ohara.client.configurator.v0.ConnectorApi.{ConnectorConfiguration, ConnectorConfigurationRequest}
import com.island.ohara.client.configurator.v0.TopicApi.TopicCreationRequest
import com.island.ohara.common.data.{Column, DataType}
import com.island.ohara.common.util.{CommonUtil, ReleaseOnce}
import com.island.ohara.integration.With3Brokers
import com.island.ohara.kafka.KafkaUtil
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
class TestCallQueue extends With3Brokers with Matchers {
  private[this] val requestTopicName = random()
  private[this] val responseTopicName = random()
  private[this] val defaultServerBuilder =
    CallQueue
      .serverBuilder()
      .brokers(testUtil.brokersConnProps)
      .requestTopic(requestTopicName)
      .responseTopic(responseTopicName)
      .groupId(com.island.ohara.common.util.CommonUtil.uuid())
  private[this] val server0: CallQueueServer[ConnectorConfigurationRequest, ConnectorConfiguration] =
    defaultServerBuilder.build[ConnectorConfigurationRequest, ConnectorConfiguration]()
  private[this] val server1: CallQueueServer[ConnectorConfigurationRequest, ConnectorConfiguration] =
    defaultServerBuilder.build[ConnectorConfigurationRequest, ConnectorConfiguration]()
  private[this] val server2: CallQueueServer[ConnectorConfigurationRequest, ConnectorConfiguration] =
    defaultServerBuilder.build[ConnectorConfigurationRequest, ConnectorConfiguration]()
  private[this] val client: CallQueueClient[ConnectorConfigurationRequest, ConnectorConfiguration] =
    CallQueue
      .clientBuilder()
      .brokers(testUtil.brokersConnProps)
      .requestTopic(requestTopicName)
      .responseTopic(responseTopicName)
      .build[ConnectorConfigurationRequest, ConnectorConfiguration]()

  private[this] val servers = Seq(server0, server1, server2)

  private[this] val requestData: ConnectorConfigurationRequest =
    ConnectorConfigurationRequest(name = "name",
                                  className = "jdbc",
                                  topics = Seq.empty,
                                  numberOfTasks = 1,
                                  schema = Seq(Column.of("cf", DataType.BOOLEAN, 1)),
                                  configs = Map("a" -> "b"))
  private[this] val responseData: ConnectorConfiguration =
    ConnectorConfiguration(
      id = "uuid",
      name = "name2",
      className = "jdbc",
      schema = Seq(Column.of("cf", DataType.BOOLEAN, 1)),
      configs = Map("a" -> "b"),
      lastModified = com.island.ohara.common.util.CommonUtil.current(),
      numberOfTasks = 1,
      topics = Seq.empty,
      state = None
    )
  private[this] val error = new IllegalArgumentException("YOU SHOULD NOT PASS")

  @Test
  def testSingleRequestWithResponse(): Unit = {
    val request = client.request(requestData)
    // no task handler so it can't get any response
    an[TimeoutException] should be thrownBy Await.result(request, 3 second)

    // wait the one from servers receive the request
    CommonUtil.await(() => servers.map(_.countOfUndealtTasks).sum == 1, Duration.ofSeconds(10))

    // get the task and assign a response
    val task = servers.find(_.countOfUndealtTasks == 1).get.take()
    task.complete(responseData)
    Await.result(request, 10 second) shouldBe Right(responseData)
  }

  @Test
  def testSingleRequestWithFailure(): Unit = {
    val request = client.request(requestData)
    // no task handler so it can't get any response
    an[TimeoutException] should be thrownBy Await.result(request, 3 second)

    // wait the one from servers receive the request
    CommonUtil.await(() => servers.map(_.countOfUndealtTasks).sum == 1, Duration.ofSeconds(10))

    // get the task and assign aCloseOnce error
    val task = servers.find(_.countOfUndealtTasks == 1).get.take()
    task.complete(error)
    val result = Await.result(request, 3 second)
    result match {
      case Left(e) => e.message shouldBe error.getMessage
      case _       => throw new RuntimeException(s"receive a invalid result: $result")
    }
  }

  @Test
  def testSingleRequestWithTimeout(): Unit = {
    val request = client.request(requestData)
    // no task handler so it can't get any response
    an[TimeoutException] should be thrownBy Await.result(request, 3 second)

    // wait the one from servers receive the request
    CommonUtil.await(() => servers.map(_.countOfUndealtTasks).sum == 1, Duration.ofSeconds(10))

    // get the server accepting the request
    val server = servers.find(_.countOfUndealtTasks == 1).get
    server.close()
    // the server is closed so all undealt tasks should be assigned a identical error
    Await.result(request, 3 second) match {
      case Left(e) => e.message shouldBe CallQueue.TERMINATE_TIMEOUT_EXCEPTION.getMessage
      case _       => throw new RuntimeException("receive a invalid result")
    }
  }

  @Test
  def testSendInvalidRequest(): Unit = {
    val invalidClient: CallQueueClient[TopicCreationRequest, ConnectorConfiguration] = CallQueue
      .clientBuilder()
      .brokers(testUtil.brokersConnProps)
      .requestTopic(requestTopicName)
      .responseTopic(responseTopicName)
      .expirationCleanupTime(3 seconds)
      .build[TopicCreationRequest, ConnectorConfiguration]()
    try {
      val request = invalidClient.request(TopicCreationRequest("uuid", 1, 2))
      Await.result(request, 5 second) match {
        case Left(e) =>
          withClue(s"exception:${e.message}") {
            e.message.contains("Unsupported type") shouldBe true
          }
        case _ => throw new RuntimeException("this request sent by this test should receive a exception")
      }

    } finally invalidClient.close()
  }

  @Test
  def testLease(): Unit = {
    val requestTopic = newTopic()
    val responseTopic = newTopic()
    val leaseCleanupFreq: scala.concurrent.duration.Duration = 5 seconds
    val timeoutClient: CallQueueClient[ConnectorConfigurationRequest, ConnectorConfiguration] = CallQueue
      .clientBuilder()
      .brokers(testUtil.brokersConnProps)
      .requestTopic(requestTopic)
      .responseTopic(responseTopic)
      .expirationCleanupTime(leaseCleanupFreq)
      .build[ConnectorConfigurationRequest, ConnectorConfiguration]()
    val request = timeoutClient.request(requestData, leaseCleanupFreq)
    TimeUnit.MILLISECONDS.sleep(leaseCleanupFreq.toMillis)
    Await.result(request, 5 second) match {
      case Left(e) => e.message shouldBe CallQueue.EXPIRED_REQUEST_EXCEPTION.getMessage
      case _       => throw new RuntimeException("this request sent by this test should receive a exception")
    }
  }

  @Test
  def testMultiRequest(): Unit = {
    val requestCount = 10
    val requests = 0 until requestCount map { _ =>
      client.request(requestData)
    }
    // wait the one from servers receive the request
    CommonUtil.await(() => servers.map(_.countOfUndealtTasks).sum == requestCount, Duration.ofSeconds(10))
    val tasks = servers.flatMap(server => {
      Iterator.continually(server.take(1 second)).takeWhile(_.isDefined).map(_.get)
    })
    tasks.size shouldBe requestCount

    tasks.foreach(_.complete(responseData))

    requests.foreach(Await.result(_, 10 seconds) match {
      case Right(r) => r shouldBe responseData
      case _        => throw new RuntimeException("All requests should work")
    })
  }

  private[this] def newTopic(): String = {
    val name = random()
    KafkaUtil.createTopic(testUtil.brokersConnProps, name, 1, 1)
    name
  }

  @After
  def tearDown(): Unit = {
    servers.foreach(ReleaseOnce.close)
    ReleaseOnce.close(client)
  }

}
