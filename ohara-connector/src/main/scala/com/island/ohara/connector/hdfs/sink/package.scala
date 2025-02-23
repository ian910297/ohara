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

package com.island.ohara.connector.hdfs

import java.util.concurrent.atomic.AtomicInteger

import com.island.ohara.common.annotations.VisibleForTesting
import com.island.ohara.common.setting.SettingDef
import com.island.ohara.kafka.connector.csv.CsvConnectorDefinitions

package object sink {

  /**
    * used to set the order of definitions.
    */
  private[this] val COUNTER = new AtomicInteger(0)
  val HDFS_URL_KEY: String = "hdfs.url"
  val HDFS_URL_DEFINITION = SettingDef
    .builder()
    .displayName("HDFS URL")
    .documentation("Input HDFS namenode location")
    .valueType(SettingDef.Type.STRING)
    .key(HDFS_URL_KEY)
    .orderInGroup(COUNTER.getAndIncrement())
    .build()

  /**
    * the core settings for HDFSSink.
    */
  val DEFINITIONS: Seq[SettingDef] = Seq(
    HDFS_URL_DEFINITION
  )

  @VisibleForTesting val TOPICS_DIR_KEY: String = CsvConnectorDefinitions.TOPICS_DIR_KEY
  @VisibleForTesting val FLUSH_SIZE_KEY: String = CsvConnectorDefinitions.FLUSH_SIZE_KEY
  @VisibleForTesting val FLUSH_SIZE_DEFAULT: Int = CsvConnectorDefinitions.FLUSH_SIZE_DEFAULT
  @VisibleForTesting val ROTATE_INTERVAL_MS_KEY: String = CsvConnectorDefinitions.ROTATE_INTERVAL_MS_KEY
  @VisibleForTesting val ROTATE_INTERVAL_MS_DEFAULT: Long = CsvConnectorDefinitions.ROTATE_INTERVAL_MS_DEFAULT
  @VisibleForTesting val FILE_NEED_HEADER_KEY: String = CsvConnectorDefinitions.FILE_NEED_HEADER_KEY
  @VisibleForTesting val FILE_NEED_HEADER_DEFAULT: Boolean = CsvConnectorDefinitions.FILE_NEED_HEADER_DEFAULT
  @VisibleForTesting val FILE_ENCODE_KEY: String = CsvConnectorDefinitions.FILE_ENCODE_KEY
  @VisibleForTesting val FILE_ENCODE_DEFAULT: String = CsvConnectorDefinitions.FILE_ENCODE_DEFAULT
}
