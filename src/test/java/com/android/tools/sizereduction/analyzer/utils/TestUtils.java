/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.tools.sizereduction.analyzer.utils;

import java.io.File;

/** Utility methods for testing the analyzer. */
public class TestUtils {
  private static final String TESTDATA_DIR =
      "src/test/resources/com/android/tools/sizereduction/analyzer/utils/testdata/";

  /**
   * Returns a File object pointing to the given testdata file.
   *
   * @param pathWithinTestdata the path within the testdata/ directory. The path should not start
   *     with a "/".
   * @return a File corresponding to the testdata filename. This tree should be treated as read-only
   */
  public static File getTestDataFile(String pathWithinTestdata) {
    File testDataFile = new File(TESTDATA_DIR + pathWithinTestdata);
    return testDataFile;
  }
}
