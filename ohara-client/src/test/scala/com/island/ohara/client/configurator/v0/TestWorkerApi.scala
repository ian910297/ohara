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

package com.island.ohara.client.configurator.v0

import com.island.ohara.client.configurator.v0.WorkerApi._
import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import org.junit.Test
import org.scalatest.Matchers
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, _}

class TestWorkerApi extends OharaTest with Matchers {

  private[this] final val accessApi =
    WorkerApi.access.hostname(CommonUtils.randomString(5)).port(CommonUtils.availablePort()).request

  @Test
  def testResponseEquals(): Unit = {
    val response = WorkerClusterInfo(
      settings = accessApi
        .brokerClusterKey(ObjectKey.of("default", CommonUtils.randomString()))
        .nodeName(CommonUtils.randomString(10))
        .creation
        .settings,
      connectors = Seq.empty,
      aliveNodes = Set.empty,
      state = None,
      error = None,
      lastModified = CommonUtils.current()
    )

    response shouldBe WORKER_CLUSTER_INFO_JSON_FORMAT.read(WORKER_CLUSTER_INFO_JSON_FORMAT.write(response))
  }

  @Test
  def testClone(): Unit = {
    val nodeNames = Set(CommonUtils.randomString())
    val workerClusterInfo = WorkerClusterInfo(
      settings = WorkerApi.access.request.nodeNames(Set(CommonUtils.randomString())).creation.settings,
      connectors = Seq.empty,
      aliveNodes = Set.empty,
      state = None,
      error = None,
      lastModified = CommonUtils.current()
    )
    workerClusterInfo.newNodeNames(nodeNames).nodeNames shouldBe nodeNames
  }

  @Test
  def ignoreNameOnCreation(): Unit =
    accessApi.nodeName(CommonUtils.randomString(10)).creation.name.length should not be 0

  @Test
  def testTags(): Unit = accessApi
    .nodeName(CommonUtils.randomString(10))
    .tags(Map("a" -> JsNumber(1), "b" -> JsString("2")))
    .creation
    .tags
    .size shouldBe 2

  @Test
  def ignoreNodeNamesOnCreation(): Unit =
    an[DeserializationException] should be thrownBy accessApi.name(CommonUtils.randomString(5)).creation

  @Test
  def nullName(): Unit = an[NullPointerException] should be thrownBy accessApi.name(null)

  @Test
  def emptyName(): Unit = an[IllegalArgumentException] should be thrownBy accessApi.name("")

  @Test
  def nullGroup(): Unit = an[NullPointerException] should be thrownBy accessApi.group(null)

  @Test
  def emptyGroup(): Unit = an[IllegalArgumentException] should be thrownBy accessApi.group("")

  @Test
  def nullBrokerClusterKey(): Unit = an[NullPointerException] should be thrownBy accessApi.brokerClusterKey(null)

  @Test
  def nullImageName(): Unit = an[NullPointerException] should be thrownBy accessApi.imageName(null)

  @Test
  def emptyImageName(): Unit = an[IllegalArgumentException] should be thrownBy accessApi.imageName("")

  @Test
  def nullNodeNames(): Unit = an[NullPointerException] should be thrownBy accessApi.nodeNames(null)

  @Test
  def emptyNodeNames(): Unit = an[IllegalArgumentException] should be thrownBy accessApi.nodeNames(Set.empty)

  @Test
  def negativeClientPort(): Unit = an[IllegalArgumentException] should be thrownBy accessApi.clientPort(-1)

  @Test
  def negativeJmxPort(): Unit = an[IllegalArgumentException] should be thrownBy accessApi.jmxPort(-1)

  @Test
  def nullConfigTopicName(): Unit = an[NullPointerException] should be thrownBy accessApi.configTopicName(null)

  @Test
  def emptyConfigTopicName(): Unit = an[IllegalArgumentException] should be thrownBy accessApi.configTopicName("")

  @Test
  def negativeNumberOfConfigTopicReplication(): Unit =
    an[IllegalArgumentException] should be thrownBy accessApi.configTopicReplications(-1)

  @Test
  def nullOffsetTopicName(): Unit = an[NullPointerException] should be thrownBy accessApi.offsetTopicName(null)

  @Test
  def emptyOffsetTopicName(): Unit = an[IllegalArgumentException] should be thrownBy accessApi.offsetTopicName("")

  @Test
  def negativeNumberOfOffsetTopicPartitions(): Unit =
    an[IllegalArgumentException] should be thrownBy accessApi.offsetTopicPartitions(-1)

  @Test
  def negativeNumberOfOffsetTopicReplication(): Unit =
    an[IllegalArgumentException] should be thrownBy accessApi.offsetTopicReplications(-1)

  @Test
  def nullStatusTopicName(): Unit = an[NullPointerException] should be thrownBy accessApi.statusTopicName(null)

  @Test
  def emptyStatusTopicName(): Unit = an[IllegalArgumentException] should be thrownBy accessApi.statusTopicName("")

  @Test
  def negativeNumberOfStatusTopicPartitions(): Unit =
    an[IllegalArgumentException] should be thrownBy accessApi.statusTopicPartitions(-1)

  @Test
  def negativeNumberOfStatusTopicReplication(): Unit =
    an[IllegalArgumentException] should be thrownBy accessApi.statusTopicReplications(-1)

  @Test
  def testCreation(): Unit = {
    val name = CommonUtils.randomString(5)
    val group = CommonUtils.randomString(10)
    val imageName = CommonUtils.randomString()
    val clientPort = CommonUtils.availablePort()
    val jmxPort = CommonUtils.availablePort()
    val brokerClusterKey = ObjectKey.of("default", CommonUtils.randomString())
    val configTopicName = CommonUtils.randomString(10)
    val configTopicReplications: Short = 2
    val offsetTopicName = CommonUtils.randomString(10)
    val offsetTopicPartitions: Int = 2
    val offsetTopicReplications: Short = 2
    val statusTopicName = CommonUtils.randomString(10)
    val statusTopicPartitions: Int = 2
    val statusTopicReplications: Short = 2
    val nodeName = CommonUtils.randomString()
    val creation = WorkerApi.access
      .hostname(CommonUtils.randomString())
      .port(CommonUtils.availablePort())
      .request
      .name(name)
      .group(group)
      .brokerClusterKey(brokerClusterKey)
      .configTopicName(configTopicName)
      .configTopicReplications(configTopicReplications)
      .offsetTopicName(offsetTopicName)
      .offsetTopicPartitions(offsetTopicPartitions)
      .offsetTopicReplications(offsetTopicReplications)
      .statusTopicName(statusTopicName)
      .statusTopicPartitions(statusTopicPartitions)
      .statusTopicReplications(statusTopicReplications)
      .imageName(imageName)
      .clientPort(clientPort)
      .jmxPort(jmxPort)
      .nodeName(nodeName)
      .creation
    creation.name shouldBe name
    creation.group shouldBe group
    creation.imageName shouldBe imageName
    creation.clientPort shouldBe clientPort
    creation.jmxPort shouldBe jmxPort
    creation.brokerClusterKey.get shouldBe brokerClusterKey
    creation.configTopicName shouldBe configTopicName
    creation.configTopicReplications shouldBe configTopicReplications
    creation.offsetTopicName shouldBe offsetTopicName
    creation.offsetTopicPartitions shouldBe offsetTopicPartitions
    creation.offsetTopicReplications shouldBe offsetTopicReplications
    creation.statusTopicName shouldBe statusTopicName
    creation.statusTopicPartitions shouldBe statusTopicPartitions
    creation.statusTopicReplications shouldBe statusTopicReplications
    creation.nodeNames.head shouldBe nodeName
  }

  @Test
  def parseCreation(): Unit = {
    val nodeName = CommonUtils.randomString()
    val creation = WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "nodeNames": ["$nodeName"]
      |  }
      |  """.stripMargin.parseJson)
    creation.name.length shouldBe LIMIT_OF_KEY_LENGTH / 2
    creation.imageName shouldBe WorkerApi.IMAGE_NAME_DEFAULT
    creation.brokerClusterKey shouldBe None
    creation.configTopicReplications shouldBe 1
    creation.offsetTopicReplications shouldBe 1
    creation.offsetTopicPartitions shouldBe 1
    creation.statusTopicReplications shouldBe 1
    creation.statusTopicPartitions shouldBe 1
    creation.nodeNames.size shouldBe 1
    creation.nodeNames.head shouldBe nodeName
    creation.jarKeys.size shouldBe 0

    val name = CommonUtils.randomString(10)
    val group = CommonUtils.randomString(10)
    val creation2 = WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "name": "$name",
      |    "group": "$group",
      |    "nodeNames": ["$nodeName"]
      |  }
      |  """.stripMargin.parseJson)
    // group is support in create cluster
    creation2.name shouldBe name
    creation2.group shouldBe group
    creation2.imageName shouldBe WorkerApi.IMAGE_NAME_DEFAULT
    creation2.brokerClusterKey shouldBe None
    creation2.configTopicReplications shouldBe 1
    creation2.offsetTopicReplications shouldBe 1
    creation2.offsetTopicPartitions shouldBe 1
    creation2.statusTopicReplications shouldBe 1
    creation2.statusTopicPartitions shouldBe 1
    creation2.nodeNames.size shouldBe 1
    creation2.nodeNames.head shouldBe nodeName
    creation2.jarKeys.size shouldBe 0
  }

  @Test
  def testUpdate(): Unit = {
    val name = CommonUtils.randomString(10)
    val group = CommonUtils.randomString(10)
    val imageName = CommonUtils.randomString()
    val clientPort = CommonUtils.availablePort()
    val nodeName = CommonUtils.randomString()

    val creation = accessApi.name(name).nodeName(nodeName).creation
    creation.name shouldBe name
    // use default values if absent
    creation.group shouldBe GROUP_DEFAULT
    creation.imageName shouldBe WorkerApi.IMAGE_NAME_DEFAULT
    creation.nodeNames shouldBe Set(nodeName)

    // initial a new update request
    val updateAsCreation = WorkerApi.access.request
      .name(name)
      // the group here is not as same as before
      // here we use update as creation
      .group(group)
      .imageName(imageName)
      .clientPort(clientPort)
      .updating
    updateAsCreation.imageName shouldBe Some(imageName)
    updateAsCreation.clientPort shouldBe Some(clientPort)
    updateAsCreation.nodeNames should not be Some(Set(nodeName))
  }

  @Test
  def parseEmptyNodeNames(): Unit =
    an[DeserializationException] should be thrownBy WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "name": "asdasd",
      |    "nodeNames": []
      |  }
      |  """.stripMargin.parseJson)
  @Test
  def parseNodeNamesOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "nodeNames": ""
      |  }
      |  """.stripMargin.parseJson)
    thrown1.getMessage should include("the value of \"nodeNames\" can't be empty string")
  }

  @Test
  def parseZeroClientPort(): Unit =
    an[DeserializationException] should be thrownBy WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "name": "name",
      |    "clientPort": 0,
      |    "nodeNames": ["n"]
      |  }
      |  """.stripMargin.parseJson)

  @Test
  def parseNegativeClientPort(): Unit =
    an[DeserializationException] should be thrownBy WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "name": "name",
      |    "clientPort": -1,
      |    "nodeNames": ["n"]
      |  }
      |  """.stripMargin.parseJson)

  @Test
  def parseLargeClientPort(): Unit =
    an[DeserializationException] should be thrownBy WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "name": "name",
      |    "clientPort": 999999,
      |    "nodeNames": ["n"]
      |  }
      |  """.stripMargin.parseJson)

  @Test
  def parseClientPortOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "nodeNames": [
      |      "node"
      |    ],
      |    "clientPort": 0
      |  }
      |  """.stripMargin.parseJson)
    thrown1.getMessage should include("the connection port must be [1024, 65535)")

    val thrown2 = the[DeserializationException] thrownBy WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "nodeNames": [
      |      "node"
      |    ],
      |    "clientPort": -9
      |  }
      |  """.stripMargin.parseJson)
    thrown2.getMessage should include("\"clientPort\" MUST be bigger than or equal to zero")

    val thrown3 = the[DeserializationException] thrownBy WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "nodeNames": [
      |      "node"
      |    ],
      |    "clientPort": 99999
      |  }
      |  """.stripMargin.parseJson)
    thrown3.getMessage should include("the connection port must be [1024, 65535), but actual port is \"99999\"")
  }

  @Test
  def parseZeroJmxPort(): Unit =
    an[DeserializationException] should be thrownBy WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "name": "name",
      |    "jmxPort": 0,
      |    "nodeNames": ["n"]
      |  }
      |  """.stripMargin.parseJson)

  @Test
  def parseNegativeJmxPort(): Unit =
    an[DeserializationException] should be thrownBy WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "name": "name",
      |    "jmxPort": -1,
      |    "nodeNames": ["n"]
      |  }
      |  """.stripMargin.parseJson)

  @Test
  def parseLargeJmxPort(): Unit =
    an[DeserializationException] should be thrownBy WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "name": "name",
      |    "jmxPort": 999999,
      |    "nodeNames": ["n"]
      |  }
      |  """.stripMargin.parseJson)

  @Test
  def parseJmxPortOnUpdate(): Unit = {
    val thrown1 = the[DeserializationException] thrownBy WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "nodeNames": [
      |      "node"
      |    ],
      |    "jmxPort": 0
      |  }
      |  """.stripMargin.parseJson)
    thrown1.getMessage should include("the connection port must be [1024, 65535)")

    val thrown2 = the[DeserializationException] thrownBy WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "nodeNames": [
      |      "node"
      |    ],
      |    "jmxPort": -9
      |  }
      |  """.stripMargin.parseJson)
    thrown2.getMessage should include("\"jmxPort\" MUST be bigger than or equal to zero")

    val thrown3 = the[DeserializationException] thrownBy WorkerApi.WORKER_CREATION_JSON_FORMAT.read(s"""
      |  {
      |    "nodeNames": [
      |      "node"
      |    ],
      |    "jmxPort": 99999
      |  }
      |  """.stripMargin.parseJson)
    thrown3.getMessage should include("the connection port must be [1024, 65535), but actual port is \"99999\"")
  }

  @Test
  def testDeadNodes(): Unit = {
    val cluster = WorkerClusterInfo(
      settings = WorkerApi.access.request.nodeNames(Set("n0", "n1")).creation.settings,
      connectors = Seq.empty,
      aliveNodes = Set("n0"),
      state = Some("running"),
      error = None,
      lastModified = CommonUtils.current()
    )
    cluster.nodeNames shouldBe Set("n0", "n1")
    cluster.deadNodes shouldBe Set("n1")
    cluster.copy(state = None).deadNodes shouldBe Set.empty
  }

  @Test
  def testFreePorts(): Unit = {
    WorkerApi.access.request.nodeName(CommonUtils.randomString()).creation.freePorts shouldBe Set.empty

    val freePorts = Set(CommonUtils.availablePort(), CommonUtils.availablePort())
    WorkerApi.access.request
      .nodeName(CommonUtils.randomString())
      .freePorts(freePorts)
      .creation
      .freePorts shouldBe freePorts
  }

  @Test
  def stringArrayToJarKeys(): Unit = {
    val key = CommonUtils.randomString()
    val updating = WorkerApi.WORKER_UPDATING_JSON_FORMAT.read(s"""
                                                  |  {
                                                  |    "jarKeys": ["$key"]
                                                  |  }
                                                  |  """.stripMargin.parseJson)
    updating.jarKeys.get.head shouldBe ObjectKey.of(GROUP_DEFAULT, key)
  }

  @Test
  def groupShouldAppearInResponse(): Unit = {
    val name = CommonUtils.randomString(5)
    val res = WorkerApi.WORKER_CLUSTER_INFO_JSON_FORMAT.write(
      WorkerClusterInfo(
        settings =
          accessApi.name(name).brokerClusterKey(ObjectKey.of("default", "bk1")).nodeNames(Set("n1")).creation.settings,
        aliveNodes = Set.empty,
        state = None,
        error = None,
        lastModified = CommonUtils.current(),
        connectors = Seq.empty
      ))

    // serialize to json should see the object key (group, name)
    res.asJsObject.fields("settings").asJsObject.fields(NAME_KEY).convertTo[String] shouldBe name
    res.asJsObject.fields("settings").asJsObject.fields(GROUP_KEY).convertTo[String] shouldBe GROUP_DEFAULT

    // deserialize to info should see the object key (group, name)
    val data = WorkerApi.WORKER_CLUSTER_INFO_JSON_FORMAT.read(res)
    data.name shouldBe name
    data.group shouldBe GROUP_DEFAULT
  }

  @Test
  def testConnectionProps(): Unit = {
    val cluster = WorkerClusterInfo(
      settings = WorkerApi.access.request.nodeNames(Set("n0", "m1")).creation.settings,
      aliveNodes = Set("nn"),
      state = None,
      error = None,
      lastModified = CommonUtils.current(),
      connectors = Seq.empty
    )
    cluster.connectionProps should not include "nn"
  }

  @Test
  def testBrokerClusterKey(): Unit = {
    val bkKey = ObjectKey.of(CommonUtils.randomString(10), CommonUtils.randomString(10))
    accessApi.nodeName("n").brokerClusterKey(bkKey).creation.brokerClusterKey.get shouldBe bkKey
  }
}
