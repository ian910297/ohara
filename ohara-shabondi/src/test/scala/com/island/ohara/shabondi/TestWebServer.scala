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

package com.island.ohara.shabondi

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.island.ohara.common.rule.OharaTest
import org.junit.Test
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, Suite}

class TestWebServer extends OharaTest with Matchers with Suite with ScalaFutures with ScalatestRouteTest {

  @Test
  def testSimple(): Unit = {
    val request = HttpRequest(uri = "/hello")

    request ~> WebServer.route ~> check {
      entityAs[String] should ===("<h1>Hello akka-http</h1>")
    }

  }
}
