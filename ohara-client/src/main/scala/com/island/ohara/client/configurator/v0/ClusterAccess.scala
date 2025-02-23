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
import com.island.ohara.common.setting.ObjectKey
import ClusterAccess.Query
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}

/**
  * the cluster-related data is different from normal data so we need another type of access.
  * @param prefixPath path to remote resource
  */
private[v0] abstract class ClusterAccess[Creation <: ClusterCreation, Updating <: ClusterUpdating, Res <: ClusterInfo](
  prefixPath: String)(implicit rm1: RootJsonFormat[Creation], rm2: RootJsonFormat[Updating], rm3: RootJsonFormat[Res])
    extends Access[Creation, Updating, Res](prefixPath) {

  def query: Query[Res]

  final def addNode(objectKey: ObjectKey, nodeName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.put[ErrorApi.Error](url(objectKey, nodeName))
  final def removeNode(objectKey: ObjectKey, nodeName: String)(
    implicit executionContext: ExecutionContext): Future[Unit] =
    exec.delete[ErrorApi.Error](url(objectKey, nodeName))

  /**
    *  start a cluster
    *
    * @param objectKey object key
    * @param executionContext execution context
    * @return none
    */
  final def start(objectKey: ObjectKey)(implicit executionContext: ExecutionContext): Future[Unit] =
    put(objectKey, START_COMMAND)

  /**
    * stop a cluster gracefully.
    *
    * @param objectKey object key
    * @param executionContext execution context
    * @return none
    */
  final def stop(objectKey: ObjectKey)(implicit executionContext: ExecutionContext): Future[Unit] =
    put(objectKey, STOP_COMMAND)

  /**
    * force to stop a cluster.
    * This action may cause some data loss if cluster was still running.
    *
    * @param objectKey object key
    * @param executionContext execution context
    * @return none
    */
  final def forceStop(objectKey: ObjectKey)(implicit executionContext: ExecutionContext): Future[Unit] =
    exec.put[ErrorApi.Error](url(key = objectKey, postFix = STOP_COMMAND, params = Map(FORCE_KEY -> "true")))
}

object ClusterAccess {

  /**
    * the basic query for cluster APIs.
    * @tparam Res cluster type
    */
  trait Query[Res <: ClusterInfo] extends BasicQuery[Res] {
    import spray.json._

    def state(value: String): Query.this.type = set("state", value)

    def aliveNodes(value: Set[String]): Query.this.type =
      set("aliveNodes", JsArray(value.map(JsString(_)).toVector).toString())

    def setting(key: String, value: JsValue): Query.this.type = set(key, value match {
      case JsString(s) => s
      case _           => value.toString
    })
  }
}
