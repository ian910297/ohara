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

package com.island.ohara.kafka.connector.csv.sink;

import com.island.ohara.common.util.StreamUtils;
import com.island.ohara.kafka.connector.RowSinkRecord;
import com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions;
import com.island.ohara.kafka.connector.csv.WithMockStorage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class TestTopicPartitionWriter extends WithMockStorage {
  private final Map<String, String> localProps = new HashMap<>();
  private final File topicsDir = createTemporaryFolder();

  private TopicPartitionWriter writer;

  @Override
  protected Map<String, String> createProps() {
    Map<String, String> props = super.createProps();
    props.put(CsvConnectorDefinitions.TOPICS_DIR_KEY, topicsDir.getPath());
    props.putAll(localProps);
    return props;
  }

  @Override
  public void setUp() {
    super.setUp();
    CsvRecordWriterProvider format = new CsvRecordWriterProvider(fs);
    writer = new TopicPartitionWriter(TOPIC_PARTITION, format, config, context);
    fs.delete(topicsDir.getPath());
  }

  @Test
  public void testWrite() {
    localProps.put(CsvConnectorDefinitions.FLUSH_SIZE_KEY, "10");
    setUp();

    List<RowSinkRecord> records = createRecords(7);
    for (RowSinkRecord record : records) {
      writer.buffer(record);
    }
    writer.write();
    Assert.assertEquals(7, writer.getRecordCount());

    records = createRecords(2);
    for (RowSinkRecord record : records) {
      writer.buffer(record);
    }
    writer.write();
    Assert.assertEquals(9, writer.getRecordCount());
  }

  @Test
  public void testWriteOnSizeRotate() {
    localProps.put(CsvConnectorDefinitions.FLUSH_SIZE_KEY, "3");

    setUp();

    List<RowSinkRecord> records = createRecords(7);
    for (RowSinkRecord record : records) {
      writer.buffer(record);
    }

    writer.write();

    Assert.assertEquals(1, writer.getRecordCount());
    Assert.assertEquals(6, writer.getCommittedOffset().intValue());
  }

  @Test
  public void testWriteOnTimeRotate() throws Exception {
    localProps.put(CsvConnectorDefinitions.FLUSH_SIZE_KEY, "99999");
    localProps.put(CsvConnectorDefinitions.ROTATE_INTERVAL_MS_KEY, "3000"); // 3 seconds

    setUp();

    List<RowSinkRecord> records = createRecords(7, 0);
    for (RowSinkRecord record : records) {
      writer.buffer(record);
    }

    writer.write();

    Assert.assertEquals(7, writer.getRecordCount());
    Assert.assertEquals(null, writer.getCommittedOffset());

    Thread.sleep(5000);

    records = createRecords(5, 7);
    for (RowSinkRecord record : records) {
      writer.buffer(record);
    }

    writer.write();

    Assert.assertEquals(5, writer.getRecordCount());
    Assert.assertEquals(7, writer.getCommittedOffset().intValue());
  }

  @Test
  public void testCommitFilename() {
    localProps.put(CsvConnectorDefinitions.FLUSH_SIZE_KEY, "3");

    setUp();

    List<RowSinkRecord> records = createRecords(7);
    for (RowSinkRecord record : records) {
      writer.buffer(record);
    }

    writer.write();
    writer.close();

    verifyFilenames("test-topic-12-000000000.csv", "test-topic-12-000000003.csv");
  }

  protected void verifyFilenames(String... filenames) {
    String encodedPartition = "partition" + TOPIC_PARTITION.partition();
    String dir = topicsDir + "/" + TOPIC_PARTITION.topicName() + "/" + encodedPartition;

    List<String> actualFilenames =
        StreamUtils.iterate(fs.listFileNames(dir)).collect(Collectors.toList());

    for (String filename : filenames) {
      Assert.assertTrue(actualFilenames.contains(filename));
    }
  }
}
