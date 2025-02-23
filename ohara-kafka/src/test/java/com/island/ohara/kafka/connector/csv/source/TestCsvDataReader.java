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

package com.island.ohara.kafka.connector.csv.source;

import com.island.ohara.common.data.Column;
import com.island.ohara.common.data.DataType;
import com.island.ohara.kafka.connector.RowSourceRecord;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class TestCsvDataReader extends WithFakeStorage {

  protected DataReader createDataReader() {
    return createDataReader(SCHEMA);
  }

  protected DataReader createDataReader(List<Column> schema) {
    return createDataReader(props, schema);
  }

  protected DataReader createDataReader(Map<String, String> props, List<Column> schema) {
    CsvSourceConfig config = CsvSourceConfig.of(props, schema);
    return CsvDataReader.of(storage, config, rowContext);
  }

  @Test
  public void testNormal() {
    setup();
    verifyFileSize(1, 0, 0);

    DataReader dataReader = createDataReader();
    List<RowSourceRecord> records = dataReader.read(INPUT_FILE.toString());

    verifyRecords(records);
    verifyFileSize(0, 1, 0);
  }

  @Test
  public void testReadWithUnsupportedType() {
    setup();
    verifyFileSize(1, 0, 0);

    List<Column> newSchema =
        Arrays.asList(
            Column.builder().name("hostname").dataType(DataType.ROW).build(),
            Column.builder().name("port").dataType(DataType.ROW).build(),
            Column.builder().name("running").dataType(DataType.ROW).build());

    DataReader dataReader = createDataReader(newSchema);
    List<RowSourceRecord> records = dataReader.read(INPUT_FILE.toString());

    Assert.assertEquals(0, records.size());
    verifyFileSize(0, 0, 1);
  }

  private void verifyFileSize(int inputSize, int completedSize, int errorSize) {
    verifyFileSizeInFolder(inputSize, INPUT_FOLDER);
    verifyFileSizeInFolder(completedSize, COMPLETED_FOLDER);
    verifyFileSizeInFolder(errorSize, ERROR_FOLDER);
  }
}
