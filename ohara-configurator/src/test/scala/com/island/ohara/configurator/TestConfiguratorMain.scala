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
import java.util.concurrent.{Executors, TimeUnit}

import com.island.ohara.common.rule.OharaTest
import com.island.ohara.common.util.{CommonUtils, Releasable}
import com.island.ohara.configurator.Configurator.Mode
import org.junit.{After, Test}
import org.scalatest.Matchers

import scala.concurrent.{ExecutionContext, Future}

class TestConfiguratorMain extends OharaTest with Matchers {

  @Test
  def illegalK8sUrl(): Unit = intercept[IllegalArgumentException] {
    Configurator.main(Array[String](Configurator.K8S_KEY, s"http://localhost:${CommonUtils.availablePort()}"))
  }.getMessage should include("unable to access")

  @Test
  def emptyK8sArgument(): Unit =
    an[IllegalArgumentException] should be thrownBy Configurator.main(Array[String](Configurator.K8S_KEY, ""))

  @Test
  def nullK8sArgument(): Unit =
    an[IllegalArgumentException] should be thrownBy Configurator.main(Array[String](Configurator.K8S_KEY))

  @Test
  def fakeWithK8s(): Unit = an[IllegalArgumentException] should be thrownBy Configurator.main(
    Array[String](Configurator.K8S_KEY, "http://localhost", Configurator.FAKE_KEY, "true"))

  @Test
  def k8sWithFake(): Unit = an[IllegalArgumentException] should be thrownBy Configurator.main(
    Array[String](Configurator.FAKE_KEY, "true", Configurator.K8S_KEY, "http://localhost"))

  @Test
  def testFakeMode(): Unit =
    runMain(
      Array[String](Configurator.HOSTNAME_KEY, "localhost", Configurator.PORT_KEY, "0", Configurator.FAKE_KEY, "true"),
      configurator => configurator.mode shouldBe Mode.FAKE
    )

  @Test
  def testSshMode(): Unit =
    runMain(Array[String](Configurator.HOSTNAME_KEY, "localhost", Configurator.PORT_KEY, "0"),
            configurator => configurator.mode shouldBe Mode.SSH)

  private[this] def runMain(args: Array[String], action: Configurator => Unit): Unit = {
    Configurator.GLOBAL_CONFIGURATOR_SHOULD_CLOSE = false
    val service = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
    Future[Unit](Configurator.main(args))(service)
    import java.time.Duration
    try {
      CommonUtils.await(() => Configurator.GLOBAL_CONFIGURATOR_RUNNING, Duration.ofSeconds(30))
      action(Configurator.GLOBAL_CONFIGURATOR)
    } finally {
      Configurator.GLOBAL_CONFIGURATOR_SHOULD_CLOSE = true
      service.shutdownNow()
      service.awaitTermination(60, TimeUnit.SECONDS)
    }
  }

  @After
  def tearDown(): Unit = {
    Configurator.GLOBAL_CONFIGURATOR_SHOULD_CLOSE = false
    Releasable.close(Configurator.GLOBAL_CONFIGURATOR)
    Configurator.GLOBAL_CONFIGURATOR == null
  }
}
