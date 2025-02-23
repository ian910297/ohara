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

package com.island.ohara.it.shabondi

import com.island.ohara.agent.k8s.K8SClient
import com.island.ohara.client.configurator.v0.ContainerApi._
import com.island.ohara.client.configurator.v0.ShabondiApi
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.it.{IntegrationTest, EnvTestingUtils}
import org.junit.{Before, Ignore, Test}
import org.scalatest.{Inside, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

// TODO: https://github.com/oharastream/ohara/issues/1008
@Ignore
class TestShabondiK8S extends IntegrationTest with Matchers with Inside {

  private val podLabelName = "shabondi"
  private val domainName = "default"
  private val hostname = "shabondi-host"
  private val podHostname = CommonUtils.randomString()

  private var k8sClient: K8SClient = _
  private var nodeName: String = _

  private def awaitResult[T](f: Future[T]): T = Await.result(f, 20 seconds)

  @Before
  def setup(): Unit = {
    k8sClient = EnvTestingUtils.k8sClient()
    val nodes = EnvTestingUtils.k8sNodes()
    if (nodes.isEmpty) skipTest("Skip shabondi IT before k8s environment fix.")
    else nodeName = nodes.head.hostname
  }

  @Test
  def testCreatAndRemovePod(): Unit = {
    // create pod
    val containerCreator = k8sClient.containerCreator()
    val containerInfoOpt = awaitResult {
      containerCreator
        .imageName(ShabondiApi.IMAGE_NAME_DEFAULT)
        .portMappings(Map(
          9090 -> 8080
        ))
        .nodeName(nodeName)
        .hostname(podHostname)
        .labelName(podLabelName)
        .domainName(domainName)
        .name(hostname)
        .create()
    }

    val containerInfo = containerInfoOpt.get
    containerInfo.portMappings should have size 1
    inside(containerInfo.portMappings.head) {
      case PortMapping(hostIp, portPairs) =>
        hostIp should be(podHostname)
        portPairs should be(Seq(PortPair(9090, 8080)))
    }

    await(() => {
      val containers = awaitResult(k8sClient.containers())
      val container = containers.filter { c =>
        c.hostname == podHostname
      }.head
      container.state == "RUNNING"
    })

//    // remove pod
    awaitResult(k8sClient.remove(podHostname))

    await(() => {
      val containers = awaitResult(k8sClient.containers())
      !containers.exists { c =>
        c.hostname == podHostname
      }
    })

  }
}
