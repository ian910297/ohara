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

package com.island.ohara.it.connector.smb

import com.island.ohara.client.filesystem.{FileSystem, FileSystemTestBase}
import com.island.ohara.common.util.CommonUtils
import org.junit.AssumptionViolatedException

class TestSmbFileSystem extends FileSystemTestBase {

  private[this] val itProps: ITSmbProps = try ITSmbProps(sys.env)
  catch {
    case e: IllegalArgumentException =>
      throw new AssumptionViolatedException(s"skip TestSmbFileSystem test, ${e.getMessage}")
  }

  override protected val fileSystem: FileSystem = FileSystem.smbBuilder
    .hostname(itProps.hostname)
    .port(itProps.port)
    .user(itProps.username)
    .password(itProps.password)
    .shareName(itProps.shareName)
    .build()

  override protected val rootDir: String = CommonUtils.randomString(10)
}
