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

package com.island.ohara.configurator.fake

import com.island.ohara.agent.{DataCollie, ServiceState, ZookeeperCollie}
import com.island.ohara.client.configurator.v0.ContainerApi.ContainerInfo
import com.island.ohara.client.configurator.v0.NodeApi
import com.island.ohara.client.configurator.v0.ZookeeperApi.ZookeeperClusterStatus

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

private[configurator] class FakeZookeeperCollie(node: DataCollie)
    extends FakeCollie[ZookeeperClusterStatus](node)
    with ZookeeperCollie {
  override def creator: ZookeeperCollie.ClusterCreator = (_, creation) =>
    if (clusterCache.asScala.exists(_._1.key == creation.key))
      Future.failed(new IllegalArgumentException(s"zookeeper can't increase nodes at runtime"))
    else
      Future.successful(
        addCluster(
          new ZookeeperClusterStatus(
            group = creation.group,
            name = creation.name,
            aliveNodes = creation.nodeNames,
            // In fake mode, we need to assign a state in creation for "GET" method to act like real case
            state = Some(ServiceState.RUNNING.name),
            error = None
          ),
          creation.imageName,
          creation.nodeNames,
          creation.ports
        ))

  override protected def doRemoveNode(previousCluster: ZookeeperClusterStatus, beRemovedContainer: ContainerInfo)(
    implicit executionContext: ExecutionContext): Future[Boolean] =
    Future.failed(
      new UnsupportedOperationException("zookeeper collie doesn't support to remove node from a running cluster"))

  override protected def doCreator(executionContext: ExecutionContext,
                                   containerName: String,
                                   containerInfo: ContainerInfo,
                                   node: NodeApi.Node,
                                   route: Map[String, String],
                                   arguments: Seq[String]): Future[Unit] =
    throw new UnsupportedOperationException("zookeeper collie doesn't support to doCreator function")

  override protected def dataCollie: DataCollie = node

  override protected def prefixKey: String = "fakezookeeper"
}
