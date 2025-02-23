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

package com.island.ohara.streams;

import com.island.ohara.common.data.Pair;
import com.island.ohara.common.data.Row;
import com.island.ohara.common.exception.OharaException;
import com.island.ohara.common.rule.OharaTest;
import com.island.ohara.common.setting.TopicKey;
import com.island.ohara.common.util.CommonUtils;
import com.island.ohara.streams.config.StreamDefUtils;
import com.island.ohara.streams.config.StreamDefinitions;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;

public class TestStreamApp extends OharaTest {

  @Test
  public void testCanFindCustomClassEntryFromInnerClass() {
    CustomStreamApp app = new CustomStreamApp();
    TopicKey fromKey = TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString());
    TopicKey toKey = TopicKey.of(CommonUtils.randomString(), CommonUtils.randomString());

    // initial all required environment
    StreamTestUtils.setOharaEnv(
        Stream.of(
                Pair.of(StreamDefUtils.NAME_DEFINITION.key(), CommonUtils.randomString(5)),
                Pair.of(StreamDefUtils.BROKER_DEFINITION.key(), CommonUtils.randomString()),
                Pair.of(
                    StreamDefUtils.FROM_TOPIC_KEYS_DEFINITION.key(),
                    TopicKey.toJsonString(Collections.singletonList(fromKey))),
                Pair.of(
                    StreamDefUtils.TO_TOPIC_KEYS_DEFINITION.key(),
                    TopicKey.toJsonString(Collections.singletonList(toKey))))
            .collect(Collectors.toMap(Pair::left, Pair::right)));
    StreamApp.runStreamApp(app.getClass());
  }

  @Test
  public void testCanDownloadJar() {
    File file = CommonUtils.createTempJar("streamApp");

    try {
      File downloadedFile = StreamApp.downloadJarByUrl(file.toURI().toURL().toString());
      Assert.assertTrue(downloadedFile.isFile());
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }

    file.deleteOnExit();
  }

  @Test(expected = OharaException.class)
  public void testWrongURLJar() {
    File file = CommonUtils.createTempJar("streamApp");
    // redundant quotes
    StreamApp.downloadJarByUrl("\"" + file.toURI().toString() + "\"");
  }

  @Test
  public void testCanFindJarEntry() {
    String projectPath = System.getProperty("user.dir");
    File file = new File(CommonUtils.path(projectPath, "build", "libs", "test-streamApp.jar"));

    try {
      Map.Entry<String, URLClassLoader> entry = StreamApp.findStreamAppEntry(file);
      Assert.assertEquals("com.island.ohara.streams.SimpleApplicationForOharaEnv", entry.getKey());
    } catch (OharaException e) {
      Assert.fail(e.getMessage());
    }
  }

  public static class CustomStreamApp extends StreamApp {
    final AtomicInteger counter = new AtomicInteger();

    @Override
    public StreamDefinitions config() {
      return StreamDefinitions.create();
    }

    @Override
    public void init() {
      int res = counter.incrementAndGet();
      // StreamApp should call init() first
      Assert.assertEquals(1, res);
    }

    @Override
    public void start(OStream<Row> ostream, StreamDefinitions streamDefinitions) {
      int res = counter.incrementAndGet();
      // StreamApp should call start() after init()
      Assert.assertEquals(2, res);
    }
  }
}
