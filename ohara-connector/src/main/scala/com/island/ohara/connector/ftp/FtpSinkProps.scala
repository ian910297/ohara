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

package com.island.ohara.connector.ftp

import com.island.ohara.kafka.connector.TaskSetting

case class FtpSinkProps(topicsDir: String,
                        needHeader: Boolean,
                        encode: String,
                        hostname: String,
                        port: Int,
                        user: String,
                        password: String) {
  def toMap: Map[String, String] = Map(
    TOPICS_DIR_KEY -> topicsDir,
    FILE_NEED_HEADER_KEY -> needHeader.toString,
    FILE_ENCODE_KEY -> encode,
    FTP_HOSTNAME_KEY -> hostname,
    FTP_PORT_KEY -> port.toString,
    FTP_USER_NAME_KEY -> user,
    FTP_PASSWORD_KEY -> password
  ).filter(_._2.nonEmpty)
}

object FtpSinkProps {
  def apply(settings: TaskSetting): FtpSinkProps = FtpSinkProps(
    topicsDir = settings.stringValue(TOPICS_DIR_KEY),
    needHeader = settings.booleanOption(FILE_NEED_HEADER_KEY).orElse(FILE_NEED_HEADER_DEFAULT),
    encode = settings.stringOption(FILE_ENCODE_KEY).orElse(FILE_ENCODE_DEFAULT),
    hostname = settings.stringValue(FTP_HOSTNAME_KEY),
    port = settings.intValue(FTP_PORT_KEY),
    user = settings.stringValue(FTP_USER_NAME_KEY),
    password = settings.stringValue(FTP_PASSWORD_KEY)
  )
}
