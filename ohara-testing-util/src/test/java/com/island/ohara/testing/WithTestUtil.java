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

package com.island.ohara.testing;

import com.island.ohara.common.rule.MediumTest;
import com.island.ohara.common.util.Releasable;
import java.util.NoSuchElementException;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Creating a test util without embedded services. If you want to use "micro" services only, you can
 * apply this class and then instantiate service manually.
 */
public abstract class WithTestUtil extends MediumTest {
  protected static OharaTestUtil util;

  @BeforeClass
  public static void beforeAll() {
    if (util != null)
      throw new NoSuchElementException(
          "The test util had been initialized!!! This happens on your tests don't run on different jvm");
    util = OharaTestUtil.of();
  }

  protected OharaTestUtil testUtil() {
    return util;
  }

  @AfterClass
  public static void afterAll() {
    Releasable.close(util);
    // we have to assign null to util since we allow junit to reuse jvm
    util = null;
  }
}
