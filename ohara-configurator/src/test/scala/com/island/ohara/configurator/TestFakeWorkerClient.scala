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

package com.island.ohara.configurator
import com.island.ohara.client.configurator.v0.ConnectorApi.State
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.{ConnectorKey, TopicKey}
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.configurator.fake.FakeWorkerClient
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
class TestFakeWorkerClient extends OharaTest with Matchers {

  private[this] def result[T](f: Future[T]): T = Await.result(f, 10 seconds)
  @Test
  def testControlConnector(): Unit = {
    val connectorKey = ConnectorKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val topicKey = TopicKey.of(CommonUtils.randomString(5), CommonUtils.randomString(5))
    val className = CommonUtils.randomString(10)
    val fake = new FakeWorkerClient()
    result(
      fake
        .connectorCreator()
        .connectorKey(connectorKey)
        .topicKey(topicKey)
        .numberOfTasks(1)
        .className(className)
        .create())

    result(fake.exist(connectorKey)) shouldBe true

    result(fake.status(connectorKey)).connector.state shouldBe State.RUNNING.name

    result(fake.pause(connectorKey))
    result(fake.status(connectorKey)).connector.state shouldBe State.PAUSED.name

    result(fake.resume(connectorKey))
    result(fake.status(connectorKey)).connector.state shouldBe State.RUNNING.name

    result(fake.delete(connectorKey))
    result(fake.exist(connectorKey)) shouldBe false
  }
}
