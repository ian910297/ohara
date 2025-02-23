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

package com.island.ohara.kafka.connector;

import com.island.ohara.common.data.Cell;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.util.CommonUtils;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Assert;
import org.junit.Test;

public class TestConnectorProps extends OharaTest {

  @Test
  public void emptyInStartingSource() {
    DumbSource connector = new DumbSource();
    // ohara auto-fill the keys with default values
    connector.start(Collections.emptyMap());
  }

  @Test
  public void emptyInStartingSink() {
    DumbSink connector = new DumbSink();
    // ohara auto-fill the keys with default values
    connector.start(Collections.emptyMap());
  }

  @Test
  public void emptyInStartingSourceTask() {
    DumbSourceTask task = new DumbSourceTask();
    assertException(NoSuchElementException.class, () -> task.start(Collections.emptyMap()));
  }

  @Test
  public void emptyInStartingSinkTask() {
    DumbSinkTask task = new DumbSinkTask();
    assertException(NoSuchElementException.class, () -> task.start(Collections.emptyMap()));
  }

  @Test
  public void testCounterInSink() {
    RowSinkTask task = new DumbSinkTask();
    String connectorName = CommonUtils.randomString();
    // we call start to initialize counter.
    task.start(Collections.singletonMap("name", connectorName));
    try {
      Assert.assertNotNull(task.rowCounter);
      Assert.assertNotNull(task.sizeCounter);
      Assert.assertEquals(task.rowCounter.group(), connectorName);
      Assert.assertEquals(task.sizeCounter.group(), connectorName);
      Assert.assertEquals(task.rowCounter.getValue(), 0);
      Assert.assertEquals(task.sizeCounter.getValue(), 0);
      Row row = Row.of(Cell.of(CommonUtils.randomString(), CommonUtils.randomString()));
      task.put(Collections.singletonList(new SinkRecord("topic", 0, null, row, null, null, 10)));
      Assert.assertEquals(task.rowCounter.getValue(), 1);
      Assert.assertNotEquals(task.sizeCounter.getValue(), 0);
    } finally {
      task.stop();
      Assert.assertTrue(task.rowCounter.isClosed());
      Assert.assertTrue(task.sizeCounter.isClosed());
    }
  }

  @Test
  public void testCounterInSource() {
    Row row = Row.of(Cell.of(CommonUtils.randomString(), CommonUtils.randomString()));
    RowSourceTask task =
        new DumbSourceTask() {
          @Override
          protected List<RowSourceRecord> _poll() {
            return Collections.singletonList(
                RowSourceRecord.builder().row(row).topicName(CommonUtils.randomString()).build());
          }
        };
    String connectorName = CommonUtils.randomString();
    // we call start to initialize counter.
    task.start(Collections.singletonMap("name", connectorName));
    try {
      Assert.assertNotNull(task.rowCounter);
      Assert.assertNotNull(task.sizeCounter);
      Assert.assertEquals(task.rowCounter.group(), connectorName);
      Assert.assertEquals(task.sizeCounter.group(), connectorName);
      Assert.assertEquals(task.rowCounter.getValue(), 0);
      Assert.assertEquals(task.sizeCounter.getValue(), 0);
      task.poll();
      Assert.assertEquals(task.rowCounter.getValue(), 1);
      Assert.assertNotEquals(task.sizeCounter.getValue(), 0);
    } finally {
      task.stop();
      Assert.assertTrue(task.rowCounter.isClosed());
      Assert.assertTrue(task.sizeCounter.isClosed());
    }
  }

  @Test
  public void testStop() {
    RowSourceTask task = new DumbSourceTask();
    // we don't call start() so all internal counters should be null
    task.stop();
  }

  @Test
  public void testInternalTaskConfigOfSource() {
    RowSourceConnector connector = new DumbSource();
    Assert.assertNull(connector.taskSetting);

    connector.start(Collections.singletonMap("name", CommonUtils.randomString()));
    Assert.assertNotNull(connector.taskSetting);
  }

  @Test
  public void testInternalTaskConfigOfSourceTask() {
    RowSourceTask task = new DumbSourceTask();
    Assert.assertNull(task.taskSetting);

    task.start(Collections.singletonMap("name", CommonUtils.randomString()));
    Assert.assertNotNull(task.taskSetting);
  }

  @Test
  public void testInternalTaskConfigOfSink() {
    RowSinkConnector connector = new DumbSink();
    Assert.assertNull(connector.taskSetting);

    connector.start(Collections.singletonMap("name", CommonUtils.randomString()));
    Assert.assertNotNull(connector.taskSetting);
  }

  @Test
  public void testInternalTaskConfigOfSinkTask() {
    RowSinkTask task = new DumbSinkTask();
    Assert.assertNull(task.taskSetting);

    task.start(Collections.singletonMap("name", CommonUtils.randomString()));
    Assert.assertNotNull(task.taskSetting);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failToCallCounterBuilderBeforeStartingSource() {
    new DumbSource().counterBuilder();
  }

  @Test(expected = IllegalArgumentException.class)
  public void failToCallCounterBuilderBeforeStartingSink() {
    new DumbSink().counterBuilder();
  }

  @Test(expected = IllegalArgumentException.class)
  public void failToCallCounterBuilderBeforeStartingSourceTask() {
    new DumbSourceTask().counterBuilder();
  }

  @Test(expected = IllegalArgumentException.class)
  public void failToCallCounterBuilderBeforeStartingSinkTask() {
    new DumbSinkTask().counterBuilder();
  }
}
