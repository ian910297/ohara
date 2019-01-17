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

import java.util.List;
import java.util.Objects;

/**
 * get TopicDescription from kafka client
 *
 * @see KafkaClient;
 */
public class TopicDescription {
  private final String name;
  private final int numberOfPartitions;
  private final short numberOfReplications;
  private final List<TopicOption> options;

  public TopicDescription(
      String name, int numberOfPartitions, short numberOfReplications, List<TopicOption> options) {
    this.name = name;
    this.numberOfPartitions = numberOfPartitions;
    this.numberOfReplications = numberOfReplications;
    this.options = options;
  }

  public String name() {
    return name;
  }

  public int numberOfPartitions() {
    return numberOfPartitions;
  }

  public short numberOfReplications() {
    return numberOfReplications;
  }

  public List<TopicOption> options() {
    return options;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TopicDescription that = (TopicDescription) o;
    return numberOfPartitions == that.numberOfPartitions
        && numberOfReplications == that.numberOfReplications
        && Objects.equals(name, that.name)
        && Objects.equals(options, that.options);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, numberOfPartitions, numberOfReplications, options);
  }
}
