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

package com.island.ohara.agent

import java.net.URL
import java.util.Objects

import com.island.ohara.agent.docker.ContainerState
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, PortMapping, PortPair}
import com.island.ohara.client.configurator.v0.FileInfoApi.FileInfo
import com.island.ohara.client.configurator.v0.NodeApi.Node
import com.island.ohara.client.configurator.v0.StreamApi.{Creation, StreamClusterInfo, StreamClusterStatus}
import com.island.ohara.client.configurator.v0.{Definition, StreamApi}
import com.island.ohara.common.setting.ObjectKey
import com.island.ohara.common.util.CommonUtils
import com.island.ohara.metrics.BeanChannel
import com.island.ohara.metrics.basic.CounterMBean
import com.island.ohara.streams.config.StreamDefUtils
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
  * An interface of controlling stream cluster.
  * It isolates the implementation of container manager from Configurator.
  */
trait StreamCollie extends Collie[StreamClusterStatus] {
  override def creator: StreamCollie.ClusterCreator =
    (executionContext, creation) => {
      implicit val exec: ExecutionContext = executionContext
      clusters().flatMap(clusters => {
        if (clusters.keys.exists(_.key == creation.key))
          Future.failed(
            new UnsupportedOperationException(s"Streamapp cluster does NOT support to add new nodes at runtime"))
        else {
          val jarInfo = creation.jarInfo.getOrElse(throw new RuntimeException("jarInfo should be defined"))
          dataCollie
            .valuesByNames[Node](creation.nodeNames)
            // the broker cluster should be defined in data creating phase already
            // here we just throw an exception for absent value to ensure everything works as expect
            .flatMap(
              nodes =>
                brokerContainers(
                  creation.brokerClusterKey.getOrElse(
                    throw new RuntimeException("broker cluster key should be defined")
                  )
                ).map(cs => (nodes, cs))
            )
            .flatMap {
              case (nodes, brokerContainers) =>
                val route = resolveHostNames(
                  (nodes.map(_.hostname)
                    ++ brokerContainers.map(_.nodeName)
                  // make sure the streamApp can connect to configurator
                    ++ Seq(jarInfo.url.getHost)).toSet
                )
                // ssh connection is slow so we submit request by multi-thread
                Future
                  .sequence(nodes.map {
                    newNode =>
                      val containerInfo = ContainerInfo(
                        nodeName = newNode.name,
                        id = Collie.UNKNOWN,
                        imageName = creation.imageName,
                        created = Collie.UNKNOWN,
                        // this fake container will be cached before refreshing cache so we make it running.
                        // other, it will be filtered later ...
                        state = ContainerState.RUNNING.name,
                        kind = Collie.UNKNOWN,
                        name = Collie.containerName(prefixKey, creation.group, creation.name, serviceName),
                        size = Collie.UNKNOWN,
                        portMappings = Seq(
                          PortMapping(
                            hostIp = Collie.UNKNOWN,
                            portPairs = Seq(
                              PortPair(
                                hostPort = creation.jmxPort,
                                containerPort = creation.jmxPort
                              )
                            )
                          )
                        ),
                        environments = creation.settings.map {
                          case (k, v) =>
                            k -> (v match {
                              // the string in json representation has quote in the beginning and end.
                              // we don't like the quotes since it obstruct us to cast value to pure string.
                              case JsString(s) => s
                              // save the json string for all settings
                              // StreamDefUtils offers the helper method to turn them back.
                              case _ => CommonUtils.toEnvString(v.toString)
                            })
                        },
                        // we should set the hostname to container name in order to avoid duplicate name with other containers
                        hostname = Collie.containerHostName(prefixKey, creation.group, creation.name, serviceName)
                      )
                      doCreator(executionContext,
                                containerInfo.name,
                                containerInfo,
                                newNode,
                                route,
                                creation.jmxPort,
                                jarInfo).map(_ => Some(containerInfo)).recover {
                        case _: Throwable =>
                          None
                      }
                  })
                  .map(_.flatten.toSeq)
                  .map { successfulContainers =>
                    val state = toClusterState(successfulContainers).map(_.name)
                    postCreate(
                      new StreamClusterStatus(
                        group = creation.group,
                        name = creation.name,
                        // no state means cluster is NOT running so we cleanup the dead nodes
                        aliveNodes = state.map(_ => successfulContainers.map(_.nodeName).toSet).getOrElse(Set.empty),
                        state = state,
                        error = None
                      ),
                      successfulContainers
                    )
                  }
            }
        }
      })
    }

  /**
    * Get all counter beans from cluster
    * @param cluster cluster
    * @return counter beans
    */
  def counters(cluster: StreamClusterInfo): Seq[CounterMBean] = cluster.aliveNodes.flatMap { node =>
    BeanChannel.builder().hostname(node).port(cluster.jmxPort).build().counterMBeans().asScala
  }.toSeq

  /**
    *
    * @return async future containing configs
    */
  /**
    * Get all '''SettingDef''' of current streamApp.
    * Note: This method intends to call a method that invokes the reflection method of streamApp.
    *
    * @param jarUrl the custom streamApp jar url
    * @return stream definition
    */
  //TODO : this workaround should be removed and use a new API instead in #2191...by Sam
  def loadDefinition(jarUrl: URL)(implicit executionContext: ExecutionContext): Future[Definition] =
    Future {
      import sys.process._
      val classpath = System.getProperty("java.class.path")
      val command =
        s"""java -cp "$classpath" ${StreamCollie.MAIN_ENTRY} ${StreamDefUtils.JAR_URL_DEFINITION
          .key()}=${jarUrl.toURI.toASCIIString} ${StreamCollie.CONFIG_KEY}"""
      val result = command.!!
      val className = result.split("=")(0)
      Definition(className, StreamDefUtils.ofJson(result.split("=")(1)).getSettingDefList.asScala)
    }.recover {
      case e: Throwable =>
        // We cannot parse the provided jar, return nothing and log it
        throw new IllegalArgumentException(
          s"the provided jar url: [$jarUrl] could not be parsed, return default settings only.",
          e)
    }

  override protected[agent] def toStatus(key: ObjectKey, containers: Seq[ContainerInfo])(
    implicit executionContext: ExecutionContext): Future[StreamClusterStatus] =
    Future.successful(
      new StreamClusterStatus(
        group = key.group(),
        name = key.name(),
        // Currently, docker and k8s has same naming rule for "Running",
        // it is ok that we use the containerState.RUNNING here.
        aliveNodes = containers.filter(_.state == ContainerState.RUNNING.name).map(_.nodeName).toSet,
        state = toClusterState(containers).map(_.name),
        error = None
      ))

  protected def dataCollie: DataCollie

  /**
    * Define prefixKey by different environment
    * @return prefix key
    */
  protected def prefixKey: String

  override val serviceName: String = StreamApi.STREAM_SERVICE_NAME

  protected def doCreator(executionContext: ExecutionContext,
                          containerName: String,
                          containerInfo: ContainerInfo,
                          node: Node,
                          route: Map[String, String],
                          jmxPort: Int,
                          jarInfo: FileInfo): Future[Unit]

  protected def postCreate(clusterStatus: StreamClusterStatus, successfulContainers: Seq[ContainerInfo]): Unit = {
    //Default do nothing
  }

  /**
    * get the containers for specific broker cluster. This method is used to update the route.
    * @param clusterKey key of broker cluster
    * @param executionContext thread pool
    * @return containers
    */
  protected def brokerContainers(clusterKey: ObjectKey)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]]
}

object StreamCollie {
  trait ClusterCreator extends Collie.ClusterCreator with StreamApi.Request {
    override def create(): Future[Unit] = {
      val request = creation
      // TODO: the to/from topics should not be empty in building creation ... However, our stream route
      // allowed user to enter empty for both fields... With a view to keeping the compatibility
      // we have to move the check from "parsing json" to "running cluster"
      // I'd say it is inconsistent to our cluster route ... by chia
      CommonUtils.requireNonEmpty(request.fromTopicKeys.asJava)
      CommonUtils.requireNonEmpty(request.toTopicKeys.asJava)
      doCreate(
        executionContext = Objects.requireNonNull(executionContext),
        creation = request
      )
    }

    protected def doCreate(executionContext: ExecutionContext, creation: Creation): Future[Unit]
  }

  /**
    * the only entry for ohara streamApp
    */
  val MAIN_ENTRY = "com.island.ohara.streams.StreamApp"

  /**
    * the flag to get/set streamApp configs for container
    */
  private[agent] val CONFIG_KEY = "CONFIG_KEY"

  /**
    * generate the jmx required properties
    *
    * @param hostname the hostname used by jmx remote
    * @param port the port used by jmx remote
    * @return jmx properties
    */
  private[agent] def formatJMXProperties(hostname: String, port: Int): Seq[String] = {
    Seq(
      "-Dcom.sun.management.jmxremote",
      "-Dcom.sun.management.jmxremote.authenticate=false",
      "-Dcom.sun.management.jmxremote.ssl=false",
      s"-Dcom.sun.management.jmxremote.port=$port",
      s"-Dcom.sun.management.jmxremote.rmi.port=$port",
      s"-Djava.rmi.server.hostname=$hostname"
    )
  }
}
