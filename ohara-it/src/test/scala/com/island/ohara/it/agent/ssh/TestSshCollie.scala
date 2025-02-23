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

package com.island.ohara.it.agent.ssh

import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.configurator.Configurator
import com.island.ohara.it.EnvTestingUtils
import com.island.ohara.it.agent.{BasicTests4Collie, ClusterNameHolder}
import com.island.ohara.it.category.SshConfiguratorGroup
import org.junit.Before
import org.junit.experimental.categories.Category

import scala.concurrent.ExecutionContext.Implicits.global
@Category(Array(classOf[SshConfiguratorGroup]))
class TestSshCollie extends BasicTests4Collie {
  override protected val nodes: Seq[Node] = EnvTestingUtils.sshNodes()
  override protected val nameHolder = ClusterNameHolder(nodes)
  override protected val configurator: Configurator = Configurator.builder.build()

  @Before
  final def setup(): Unit = if (nodes.isEmpty) skipTest(s"You must assign nodes for collie tests")
  else {
    val nodeApi = NodeApi.access.hostname(configurator.hostname).port(configurator.port)
    nodes.foreach { node =>
      result(
        nodeApi.request.hostname(node.hostname).port(node._port).user(node._user).password(node._password).create())
    }
    result(nodeApi.list()).size shouldBe nodes.size
    result(nodeApi.list()).foreach(node => nodes.exists(_.name == node.name) shouldBe true)
  }
}
