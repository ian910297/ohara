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

package com.island.ohara.it.client

import com.island.ohara.client.configurator.v0.LogApi
import org.junit.Test

import scala.concurrent.ExecutionContext.Implicits.global

class TestQueryConfiguratorLog extends WithRemoteConfigurator {

  @Test
  def test(): Unit = {
    val log = result(LogApi.access.hostname(configuratorHostname).port(configuratorPort).log4Configurator())
    log.clusterKey.name() shouldBe configuratorContainerName
    log.logs.size shouldBe 1
    log.logs.head.hostname shouldBe configuratorHostname
    log.logs.head.value.length should not be 0
  }
}
