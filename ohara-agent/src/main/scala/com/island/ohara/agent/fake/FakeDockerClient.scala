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

package com.island.ohara.agent.fake

import java.util.concurrent.ConcurrentHashMap
import java.util.{Date, Objects}

import com.island.ohara.agent.docker.DockerClient.ContainerInspector
import com.island.ohara.agent.docker.{ContainerCreator, ContainerState, DockerClient, NetworkDriver}
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, ContainerName, PortMapping, PortPair}
import com.island.ohara.common.util.{CommonUtils, ReleaseOnce}
import com.typesafe.scalalogging.Logger

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
private[agent] class FakeDockerClient(nodeName: String) extends ReleaseOnce with DockerClient {
  private val LOG = Logger(classOf[FakeDockerClient])
  private[this] val FAKE_KIND_NAME: String = "FAKE"
  private[this] val cacheConfigs = new ConcurrentHashMap[String, Map[String, String]]()
  private[this] val cachedContainers = new ConcurrentHashMap[String, ContainerInfo]()

  override def containerNames(): Seq[ContainerName] =
    cachedContainers.keys.asScala
      .map(
        n =>
          new ContainerName(
            id = "unknown",
            name = n,
            imageName = "unknown",
            nodeName = CommonUtils.hostname()
        ))
      .toSeq

  private[this] def listContainers(nameFilter: String => Boolean): Future[Seq[ContainerInfo]] =
    Future.successful(
      cachedContainers.asScala
        .filter {
          case (name, _) => nameFilter(name)
        }
        .values
        .toSeq)

  override def containers(nameFilter: String => Boolean)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] = listContainers(nameFilter)

  //there is no meaning of "active" in fake mode
  override def activeContainers(nameFilter: String => Boolean)(
    implicit executionContext: ExecutionContext): Future[Seq[ContainerInfo]] = listContainers(nameFilter)

  override def containerCreator(): ContainerCreator = (hostname: String,
                                                       imageName: String,
                                                       name: String,
                                                       _: String,
                                                       _: Boolean,
                                                       ports: Map[Int, Int],
                                                       envs: Map[String, String],
                                                       _: Map[String, String],
                                                       _: Map[String, String],
                                                       _: NetworkDriver) => {
    cachedContainers.put(
      name,
      ContainerInfo(
        nodeName = nodeName,
        id = name,
        imageName = imageName,
        created = new Date(CommonUtils.current()).toString,
        state = ContainerState.RUNNING.name,
        kind = FAKE_KIND_NAME,
        name = name,
        size = "-999 MB",
        portMappings =
          if (ports.isEmpty) Seq.empty
          else
            Seq(PortMapping(hostname, ports.map {
              case (port, containerPort) =>
                PortPair(port, containerPort)
            }.toSeq)),
        environments = envs,
        hostname = hostname
      )
    )
  }

  override def stop(name: String): Unit =
    cachedContainers.put(name, cachedContainers.get(name).copy(state = ContainerState.EXITED.name))

  override def remove(name: String): Unit = cachedContainers.remove(name)

  override def forceRemove(name: String): Unit = cachedContainers.remove(name)

  override def verify(): Boolean = true

  override def log(name: String): String = s"fake docker log for $name"

  override def addConfig(name: String, configs: Map[String, String]): String = {
    cacheConfigs.put(name, configs)
    name
  }

  override def inspectConfig(name: String): Map[String, String] = {
    val res = cacheConfigs.get(name)
    if (res == null) {
      throw new IllegalArgumentException(s"the required configs of $name not found!")
    }
    res
  }

  override def removeConfig(name: String): Boolean = {
    val res = cacheConfigs.remove(name)
    if (res == null) false
    else true
  }

  override def forceRemoveConfig(name: String): Boolean = removeConfig(name)

  override def containerInspector(containerName: String): ContainerInspector = containerInspector(containerName, false)

  private[this] def containerInspector(containerName: String, beRoot: Boolean): ContainerInspector =
    new ContainerInspector {
      private[this] def rootConfig: String = if (beRoot) "-u root" else ""
      override def cat(path: String): Option[String] =
        Some(s"""docker exec $rootConfig $containerName /bin/bash -c \"cat $path\"""")

      override def append(path: String, content: Seq[String]): String = {
        LOG.info(
          s"""docker exec $rootConfig $containerName /bin/bash -c \"echo \\"${content.mkString("\n")}\\" >> $path\"""")
        cat(path).get
      }

      override def write(path: String, content: Seq[String]): String = {
        LOG.info(
          s"""docker exec $rootConfig $containerName /bin/bash -c \"echo \\"${content.mkString("\n")}\\" > $path\"""")
        cat(path).get
      }

      override def asRoot(): ContainerInspector = containerInspector(containerName, true)
    }

  override def imageNames(): Seq[String] = cachedContainers.values.asScala.map(_.imageName).toSeq

  override def toString: String = getClass.getName

  override protected def doClose(): Unit = LOG.info("close client")

  override def container(name: String): ContainerInfo = Objects.requireNonNull(cachedContainers.get(name))
}
