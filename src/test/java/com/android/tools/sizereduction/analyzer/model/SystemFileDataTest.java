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

package com.android.tools.sizereduction.analyzer.model;

import static com.google.common.truth.Truth.assertThat;

import com.android.tools.sizereduction.analyzer.utils.TestUtils;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class SystemFileDataTest {

  private static final String PATH = "app.aab";

  @Test
  public void suppliesInputStream() throws Exception {
    File file = TestUtils.getTestDataFile(PATH);
    SystemFileData systemFileData = new SystemFileData(file, Paths.get("foobar"));

    try (InputStream stream = systemFileData.getInputStream()) {
      stream.read();
      assertThat(stream).isNotNull();
    }
  }

  @Test
  public void givesPathWithinRoot() throws Exception {
    File file = TestUtils.getTestDataFile(PATH);
    Path pathWithinRoot = Paths.get("hello/world/foobar.txt");
    SystemFileData systemFileData = new SystemFileData(file, pathWithinRoot);

    // This (Object) cast is required to disambiguate Path assertions,
    // see https://github.com/google/truth/issues/285
    assertThat((Object) systemFileData.getPathWithinRoot()).isEqualTo(pathWithinRoot);
  }

  @Test
  public void givesPathWithinModule() throws Exception {
    File file = TestUtils.getTestDataFile(PATH);
    Path pathWithinModule = Paths.get("/world/foobar.txt");
    SystemFileData systemFileData = new SystemFileData(file, null, pathWithinModule);

    assertThat((Object) systemFileData.getPathWithinModule()).isEqualTo(pathWithinModule);
  }

  @Test
  public void givesPathWithinModule_sameAsRoot() throws Exception {
    File file = TestUtils.getTestDataFile(PATH);
    Path rootPath = Paths.get("foobar.txt");
    SystemFileData systemFileData = new SystemFileData(file, rootPath);

    assertThat((Object) systemFileData.getPathWithinModule()).isEqualTo(rootPath);
    assertThat((Object) systemFileData.getPathWithinRoot()).isEqualTo(rootPath);
  }

  @Test
  public void getsCorrectSize() throws Exception {
    File file = TestUtils.getTestDataFile(PATH);
    SystemFileData systemFileData = new SystemFileData(file, Paths.get("foobar.txt"));

    assertThat(systemFileData.getSize()).isEqualTo(1408431L);
  }
}
