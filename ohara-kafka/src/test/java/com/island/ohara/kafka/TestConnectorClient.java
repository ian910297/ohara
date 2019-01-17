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

package com.island.ohara.kafka;

import com.island.ohara.client.ConnectorClient;
import com.island.ohara.client.ConnectorClient$;
import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.ConnectorState;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.data.Serializer;
import com.island.ohara.common.util.CommonUtil;
import com.island.ohara.integration.OharaTestUtil;
import com.island.ohara.integration.With3Brokers3Workers;
import java.time.Duration;
import java.util.List;
import org.junit.Test;

public class TestConnectorClient extends With3Brokers3Workers {
  private final OharaTestUtil testUtil = testUtil();

  private final ConnectorClient connectorClient =
      ConnectorClient$.MODULE$.apply(testUtil.workersConnProps());
  //  @Ignore
  @Test
  public void testExist() {
    String topicName = methodName();
    String connectorName = methodName();
    assertFalse(connectorClient.exist(connectorName));

    connectorClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(MyConnector.class)
        .name(connectorName)
        .numberOfTasks(1)
        .disableConverter()
        .create();

    try {
      CommonUtil.await(() -> connectorClient.exist(connectorName), Duration.ofSeconds(50));
    } finally {
      connectorClient.delete(connectorName);
    }
  }

  @Test
  public void testExistOnUnrunnableConnector() {
    String topicName = methodName();
    String connectorName = methodName();
    assertFalse(connectorClient.exist(connectorName));

    connectorClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(UnrunnableConnector.class)
        .name(connectorName)
        .numberOfTasks(1)
        .disableConverter()
        .create();

    try {
      CommonUtil.await(() -> connectorClient.exist(connectorName), Duration.ofSeconds(50));
    } finally {
      connectorClient.delete(connectorName);
    }
  }

  @Test
  public void testPauseAndResumeSource() {
    String topicName = methodName();
    String connectorName = methodName();
    connectorClient
        .connectorCreator()
        .topic(topicName)
        .connectorClass(MyConnector.class)
        .name(connectorName)
        .numberOfTasks(1)
        .disableConverter()
        .create();
    CommonUtil.await(() -> connectorClient.exist(connectorName), Duration.ofSeconds(50));
    try (Consumer<byte[], Row> consumer =
        Consumer.builder()
            .topicName(topicName)
            .offsetFromBegin()
            .brokers(testUtil.brokersConnProps())
            .build(Serializer.BYTES, Serializer.ROW)) {

      // try to receive some data from topic
      List<ConsumerRecord<byte[], Row>> result = consumer.poll(Duration.ofSeconds(10), 1);
      assertNotEquals(result.size(), 0);
      result.forEach(x -> assertEquals(x.value().get(), ROW));

      // pause connector
      connectorClient.pause(connectorName);
      CommonUtil.await(
          () -> connectorClient.status(connectorName).connector().state() == ConnectorState.PAUSED,
          Duration.ofSeconds(50));

      // try to receive all data from topic...10 seconds should be enough in this case;
      result = consumer.poll(Duration.ofSeconds(10), Integer.MAX_VALUE);
      result.forEach(x -> assertEquals(x.value().get(), ROW));

      // connector is paused so there is no data
      result = consumer.poll(Duration.ofSeconds(20), 1);
      assertEquals(result.size(), 0);

      // resume connector
      connectorClient.resume(connectorName);
      CommonUtil.await(
          () -> connectorClient.status(connectorName).connector().state() == ConnectorState.RUNNING,
          Duration.ofSeconds(50),
          Duration.ofSeconds(2),
          true);

      // since connector is resumed so some data are generated
      result = consumer.poll(Duration.ofSeconds(20), 1);
      assertNotEquals(result.size(), 0);
    } finally {
      connectorClient.delete(connectorName);
    }
  }

  static final Row ROW = Row.of(Cell.of("f0", 13), Cell.of("f1", false));
}
