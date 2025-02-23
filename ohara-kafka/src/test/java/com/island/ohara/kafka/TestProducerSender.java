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

import com.island.ohara.common.rule.OharaTest;
import java.util.Collections;
import java.util.concurrent.Future;
import org.junit.Test;

public class TestProducerSender extends OharaTest {

  private static class FakeSender<K, V> extends Producer.Sender<K, V> {
    @Override
    public Future<Producer.RecordMetadata> doSend() {
      return null;
    }
  }

  private static FakeSender<String, String> fake() {
    return new FakeSender<>();
  }

  @Test(expected = NullPointerException.class)
  public void nullHeader() {
    fake().header(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullHeaders() {
    fake().headers(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyHeaders() {
    fake().headers(Collections.emptyList());
  }

  @Test(expected = NullPointerException.class)
  public void nullKey() {
    fake().key(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullValue() {
    fake().value(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullTopicName() {
    fake().topicName(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyTopicName() {
    fake().topicName("");
  }
}
