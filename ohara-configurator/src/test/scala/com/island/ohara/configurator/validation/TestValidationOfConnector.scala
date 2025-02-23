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

package com.island.ohara.configurator.validation

import com.island.ohara.client.configurator.v0.{ValidationApi, WorkerApi}
import com.island.ohara.common.setting.TopicKey
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.{Configurator, DumbSink}
import com.island.ohara.kafka.connector.json.ConnectorDefUtils
import com.island.ohara.testing.With3Brokers3Workers
import org.junit.{After, Test}
import org.scalatest.Matchers
import spray.json.{JsString, _}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestValidationOfConnector extends With3Brokers3Workers with Matchers {
  private[this] val configurator =
    Configurator.builder.fake(testUtil().brokersConnProps(), testUtil().workersConnProps()).build()

  private[this] val wkCluster = result(WorkerApi.access.hostname(configurator.hostname).port(configurator.port).list()).head

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)

  @Test
  def goodCase(): Unit = {
    val topicKeys = Set(TopicKey.of(CommonUtils.randomString(10), CommonUtils.randomString(10)))
    val response = result(
      ValidationApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .connectorRequest
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .numberOfTasks(1)
        .workerClusterKey(wkCluster.key)
        .topicKeys(topicKeys)
        .verify())
    response.className.get() shouldBe classOf[DumbSink].getName
    response.settings().size() should not be 0
    response.numberOfTasks().get() shouldBe 1
    import scala.collection.JavaConverters._
    response.topicKeys().asScala.toSet shouldBe topicKeys
    response.author().isPresent shouldBe true
    response.version().isPresent shouldBe true
    response.revision().isPresent shouldBe true
    response.workerClusterKey().isPresent shouldBe true
    response.connectorType().isPresent shouldBe true
    response.errorCount() shouldBe 0
  }

  /**
    * the classname is required so the request will be rejected directly
    */
  @Test
  def ignoreClassName(): Unit = an[DeserializationException] should be thrownBy result(
    ValidationApi.access
      .hostname(configurator.hostname)
      .port(configurator.port)
      .connectorRequest
      .name(CommonUtils.randomString(10))
      .numberOfTasks(1)
      .workerClusterKey(wkCluster.key)
      .topicKey(TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString()))
      .verify())

  @Test
  def ignoreTopicName(): Unit = an[IllegalArgumentException] should be thrownBy result(
    ValidationApi.access
      .hostname(configurator.hostname)
      .port(configurator.port)
      .connectorRequest
      .name(CommonUtils.randomString(10))
      .className(classOf[DumbSink].getName)
      .numberOfTasks(1)
      .workerClusterKey(wkCluster.key)
      .verify())

  @Test
  def ignoreNumberOfTasks(): Unit = {
    val response = result(
      ValidationApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .connectorRequest
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .topicKey(TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString()))
        .workerClusterKey(wkCluster.key)
        .verify())
    response.className.get() shouldBe classOf[DumbSink].getName
    response.settings().size() should not be 0
    response.numberOfTasks().get shouldBe 1
    response.topicNamesOnKafka().size() shouldBe 1
    response.author().isPresent shouldBe true
    response.version().isPresent shouldBe true
    response.revision().isPresent shouldBe true
    response.workerClusterKey().get shouldBe wkCluster.key
    response.connectorType().isPresent shouldBe true
    response.errorCount() shouldBe 0
  }

  @Test
  def testTags(): Unit = {
    val tags = Map("a" -> JsString("b"))
    val response = result(
      ValidationApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .connectorRequest
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .topicKey(TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString()))
        .workerClusterKey(wkCluster.key)
        .tags(tags)
        .verify())
    response.value(ConnectorDefUtils.TAGS_DEFINITION.key()).get().parseJson shouldBe JsObject(Map("a" -> JsString("b")))
    response
      .settings()
      .asScala
      .filter(_.definition().key() == ConnectorDefUtils.TAGS_DEFINITION.key())
      .head
      .definition()
      .defaultValue() shouldBe null

    response.errorCount() shouldBe 0

    // no tags
    val response2 = result(
      ValidationApi.access
        .hostname(configurator.hostname)
        .port(configurator.port)
        .connectorRequest
        .name(CommonUtils.randomString(10))
        .className(classOf[DumbSink].getName)
        .topicKey(TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString()))
        .workerClusterKey(wkCluster.key)
        .verify())
    response2.errorCount() shouldBe 0
  }

  @After
  def tearDown(): Unit = Releasable.close(configurator)
}
