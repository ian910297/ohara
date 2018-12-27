package com.island.ohara.demo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.island.ohara.client.ConfiguratorJson.PRIVATE_API
import com.island.ohara.common.rule.LargeTest
import com.island.ohara.common.util.CommonUtil
import com.island.ohara.demo.Backend._
import org.junit.Test
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TestBackendServicesOnSpecifiedPorts extends LargeTest with Matchers {

  @Test
  def test(): Unit = {
    val ports = ServicePorts(
      dbPort = CommonUtil.availablePort(),
      ftpPort = CommonUtil.availablePort(),
      ftpDataPorts = Seq(CommonUtil.availablePort()),
      configuratorPort = CommonUtil.availablePort(),
      zkPort = CommonUtil.availablePort(),
      brokersPort = Seq.fill(3)(CommonUtil.availablePort()).toArray,
      workersPort = Seq.fill(3)(CommonUtil.availablePort()).toArray
    )
    Backend.run(
      ports,
      (configurator, _, _, _, database, ftp) => {
        implicit val actorSystem: ActorSystem = ActorSystem(methodName)
        try {
          implicit val materializer: ActorMaterializer = ActorMaterializer()
          val result = Await.result(
            Http()
              .singleRequest(
                HttpRequest(HttpMethods.GET, s"http://localhost:${configurator.port}/$PRIVATE_API/services"))
              .flatMap(res => {
                if (res.status.isSuccess()) Unmarshal(res.entity).to[Services]
                else
                  Future.failed(new IllegalArgumentException(s"Failed to create table. error:${res.status.intValue()}"))
              }),
            20 seconds
          )
          result.zookeeper shouldBe s"${CommonUtil.hostname}:${ports.zkPort}"
          result.brokers shouldBe ports.brokersPort.map(p => s"${CommonUtil.hostname}:$p").mkString(",")
          result.workers shouldBe ports.workersPort.map(p => s"${CommonUtil.hostname}:$p").mkString(",")

          result.ftpServer.hostname shouldBe ftp.hostname
          result.ftpServer.port shouldBe ports.ftpPort
          result.ftpServer.dataPort shouldBe ports.ftpDataPorts
          result.ftpServer.user shouldBe ftp.user
          result.ftpServer.password shouldBe ftp.password

          result.database.url shouldBe database.url
          result.database.user shouldBe database.user
          result.database.password shouldBe database.password
        } finally actorSystem.terminate()
      }
    )
  }
}
