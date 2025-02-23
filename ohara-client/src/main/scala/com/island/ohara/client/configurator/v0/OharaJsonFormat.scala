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

import spray.json.{JsValue, RootJsonFormat}

/**
  * Except for akka json function, ohara format expose the function used to verify the input key and value. The function
  * is useful in testing single item. For example, the information, which is carried by url, to restful APIs can be verified
  * by the check method.
  *
  * The exposed check make us have consistent behavior in parsing string to scala object. For example, the name is placed
  * at both url and payload, and both of them must go through the same name string check.
  * @tparam T object
  */
trait OharaJsonFormat[T] extends RootJsonFormat[T] {

  /**
    * verify the input key and value. It always pass if the input key is not associated to any check rule.
    * @param key input key
    * @param value input value
    */
  def check[Value <: JsValue](key: String, value: Value): Value = check(Map(key -> value))(key).asInstanceOf[Value]

  /**
    * verify the input keys and values. It always pass if the input keys are not associated to any check rule.
    * @param fields keys and values
    */
  def check(fields: Map[String, JsValue]): Map[String, JsValue]
}
