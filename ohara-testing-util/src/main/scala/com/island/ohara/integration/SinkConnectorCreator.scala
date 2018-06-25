package com.island.ohara.integration

import com.island.ohara.rest.RestResponse
import org.apache.kafka.connect.sink.SinkConnector

/**
  * Used to config and run the sink connector.
  */
trait SinkConnectorCreator {

  /**
    * config the converter be org.apache.kafka.connect.converters.ByteArrayConverter. It is useful if the data in topic
    * your connector want to take is byte array and is generated by kafka producer. For example, the source is RowProducer,
    * and the target is RowSinkConnector.
    *
    * @return this one
    */
  def disableConverter: SinkConnectorCreator

  /**
    * set the connector name. It should be a unique name.
    * @param name connector name
    * @return this one
    */
  def name(name: String): SinkConnectorCreator

  /**
    * set the connector class. The class must be loaded in class loader otherwise it will fail to create the connector.
    * @param clz connector class
    * @return this one
    */
  def connectorClass(clz: Class[_ <: SinkConnector]): SinkConnectorCreator

  /**
    * set the topic in which you have interest.
    * @param topicName topic
    * @return this one
    */
  def topic(topicName: String): SinkConnectorCreator = topics(Seq(topicName))

  /**
    * set the topics in which you have interest.
    * @param topicNames topics
    * @return this one
    */
  def topics(topicNames: Seq[String]): SinkConnectorCreator

  /**
    * the max number of sink task you want to create
    * @param taskMax max number of sink task
    * @return this one
    */
  def taskNumber(taskMax: Int): SinkConnectorCreator

  /**
    * extra config passed to sink connector. This config is optional.
    * @param config config
    * @return this one
    */
  def config(config: Map[String, String]): SinkConnectorCreator

  /**
    * send the request to create the sink connector.
    * @return this one
    */
  def run(): RestResponse
}
