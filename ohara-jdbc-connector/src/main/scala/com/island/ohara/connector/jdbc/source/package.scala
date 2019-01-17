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

package com.island.ohara.connector.jdbc

package object source {
  val DB_URL: String = "source.db.url"
  val DB_USERNAME: String = "source.db.username"
  val DB_PASSWORD: String = "source.db.password"
  val DB_TABLENAME: String = "source.table.name"
  val DB_CATALOG_PATTERN: String = "source.schema.catalog"
  val DB_SCHEMA_PATTERN: String = "source.schema.pattern"
  val MODE: String = "mode"
  val MODE_DEFAULT = "timestamp"
  val TIMESTAMP_COLUMN_NAME: String = "source.timestamp.column.name"
}
