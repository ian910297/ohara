package com.island.ohara.agent

import java.util.Objects

import com.island.ohara.agent.DockerClient.{ContainerCreator, ContainerInspector}
import com.island.ohara.client.configurator.v0.ContainerApi.{ContainerInfo, ContainerState}
import com.island.ohara.common.annotations.Optional
import com.island.ohara.common.util.Releasable

/**
  * a interface used to control remote node's docker service.
  * the default implementation is based on ssh client.
  * NOTED: All containers are executed background so as to avoid blocking call.
  */
trait DockerClient extends Releasable {

  /**
    * @param name container's name
    * @return true if container exists. otherwise, false
    */
  def exist(name: String): Boolean = containers().exists(_.name == name)

  /**
    * @param name container's name
    * @return true if container does not exist. otherwise, true
    */
  def nonExist(name: String): Boolean = !exist(name)

  /**
    * @return a collection of running docker containers
    */
  def activeContainers(): Seq[ContainerInfo] = containers().filter(_.state == ContainerState.RUNNING)

  /**
    * @return a collection of docker containers
    */
  def containers(): Seq[ContainerInfo]

  /**
    * @param name container's name
    * @return container description or None if container doesn't exist
    */
  def container(name: String): Option[ContainerInfo] = containers().find(_.name == name)

  /**
    * @param name container's id
    * @return container description or None if container doesn't exist
    */
  def containerById(id: String): Option[ContainerInfo] = containers().find(_.id == id)

  /**
    * start a docker container.
    * @return a executor
    */
  def containerCreator(): ContainerCreator

  /**
    * stop a running container. If the container doesn't exist, exception will be thrown.
    * Noted: Please use #stopById to stop container by id
    * @param name container's name
    * @return container information.
    */
  def stop(name: String): ContainerInfo

  /**
    * remove a stopped container. If the container doesn't exist, exception will be thrown.
    * Noted: Please use #stopById to remove container by id
    * @param name container's name
    * @return container information.
    */
  def remove(name: String): ContainerInfo

  /**
    * remove a container. If the container doesn't exist, exception will be thrown.
    * @param name container's name
    * @return container information.
    */
  def forceRemove(name: String): ContainerInfo

  /**
    * stop a running container. If the container doesn't exist, exception will be thrown.
    * Noted: Please use #stop to stop container by name
    * @param id container's id
    * @return container information.
    */
  def stopById(id: String): ContainerInfo

  /**
    * remove a stopped container. If the container doesn't exist, exception will be thrown.
    * Noted: Please use #stop to remove container by name
    * @param id container's id
    * @return container information.
    */
  def removeById(id: String): ContainerInfo

  /**
    * @param id container's id
    * @return true if container exists. otherwise, false
    */
  def existById(id: String): Boolean = containers().exists(_.id == id)

  /**
    * @param id container's id
    * @return true if container does not exist. otherwise, true
    */
  def nonExistById(id: String): Boolean = !existById(id)

  /**
    * check whether the remote node is capable of running docker.
    * @return true if remote node succeed to run hello-world container. otherwise, false.
    */
  def verify(): Boolean

  /**
    * get the console log from the container
    * @param name container's name
    * @return log
    */
  def log(name: String): String

  def images(): Seq[String]

  def containerInspector(containerName: String): ContainerInspector
}

object DockerClient {
  def builder(): Builder = new Builder

  /**
    * used to "touch" a running container. For example, you can cat a file from a running container
    */
  trait ContainerInspector {

    /**
      * convert the user to root. If the files accessed by inspect requires the root permission, you can run this method
      * before doing inspect action.
      * @return a new ContainerInspector with root permission
      */
    def asRoot(): ContainerInspector

    /**
      * get content of specified file from a container.
      * This method is useful in debugging when you want to check something according to the file content.
      * @param path file path
      * @return content of file
      */
    def cat(path: String): Option[String]

    /**
      * append something to the file of a running container
      * @param content content
      * @param path file path
      */
    def append(path: String, content: String): String = append(path, Seq(content))

    /**
      * append something to the file of a running container
      * @param content content
      * @param path file path
      */
    def append(path: String, content: Seq[String]): String

    /**
      * clear and write something to the file of a running container
      * @param content content
      * @param path file path
      */
    def write(path: String, content: String): String = write(path, Seq(content))

    /**
      * clear and write something to the file of a running container
      * @param content content
      * @param path file path
      */
    def write(path: String, content: Seq[String]): String
  }

  /**
    * a interface used to run a docker container on remote node
    */
  trait ContainerCreator {

    /**
      * set true if you want to clean up the dead container automatically
      * @return executor
      */
    def cleanup(): ContainerCreator

    /**
      * set container's name. default is a random string
      * @param name container name
      * @return this executor
      */
    def name(name: String): ContainerCreator

    /**
      * set target image
      * @param imageName docker image
      * @return this executor
      */
    def imageName(imageName: String): ContainerCreator

    /**
      * the command passed to docker container
      * @param command command
      * @return this executor
      */
    def command(command: String): ContainerCreator

    /**
      * @param hostname the hostname of container
      * @return this executor
      */
    def hostname(hostname: String): ContainerCreator

    /**
      * @param envs the env variables exposed to container
      * @return this executor
      */
    def envs(envs: Map[String, String]): ContainerCreator

    /**
      * @param route the pre-defined route to container. hostname -> ip
      * @return this executor
      */
    def route(route: Map[String, String]): ContainerCreator

    /**
      * forward the port from host to container.
      * NOTED: currently we don't support to specify the network interface so the forwarded port is bound on all networkd adapters.
      * @param ports port mapping (host's port -> container's port)
      * @return this executor
      */
    def portMappings(ports: Map[Int, Int]): ContainerCreator

    /**
      * docker -v
      * @return process information
      */
    def volumnMapping(ports: Map[String, String]): ContainerCreator

    /**
      * execute the docker container on background
      * @return process information
      */
    def run(): Option[ContainerInfo]

    /**
      * this is used in testing. Devlopers can check the generated command by this method.
      * @return the command used to start docker container
      */
    protected[agent] def dockerCommand(): String
  }

  class Builder private[agent] {
    private[this] var hostname: String = _
    private[this] var port: Int = 22
    private[this] var user: String = _
    private[this] var password: String = _

    def hostname(hostname: String): Builder = {
      this.hostname = hostname
      this
    }

    @Optional("default port is 22")
    def port(port: Int): Builder = {
      this.port = port
      this
    }

    def user(user: String): Builder = {
      this.user = user
      this
    }

    def password(password: String): Builder = {
      this.password = password
      this
    }

    def build(): DockerClient = new DockerClientImpl(Objects.requireNonNull(hostname),
                                                     port,
                                                     Objects.requireNonNull(user),
                                                     Objects.requireNonNull(password))
  }
}
