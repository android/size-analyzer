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
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class ZipFileDataTest {

  private static final String APP_BUNDLE = "app.aab";

  @Test
  public void suppliesInputStream() throws Exception {
    File bundleFile = TestUtils.getTestDataFile(APP_BUNDLE);
    try (ZipFile zipFile = new ZipFile(bundleFile)) {
      ZipEntry entry = zipFile.getEntry("base/manifest/AndroidManifest.xml");
      ZipFileData data = new ZipFileData(zipFile, entry);

      try (InputStream stream = data.getInputStream()) {
        stream.read();
        assertThat(stream).isNotNull();
      }
    }
  }

  @Test
  public void givesPathWithinRoot() throws Exception {
    File bundleFile = TestUtils.getTestDataFile(APP_BUNDLE);
    try (ZipFile zipFile = new ZipFile(bundleFile)) {
      ZipEntry entry = zipFile.getEntry("base/manifest/AndroidManifest.xml");
      ZipFileData data = new ZipFileData(zipFile, entry);

      // This (Object) cast is required to disambiguate Path assertions,
      // see https://github.com/google/truth/issues/285
      assertThat((Object) data.getPathWithinRoot())
          .isEqualTo(Paths.get("base/manifest/AndroidManifest.xml"));
    }
  }

  @Test
  public void givesPathWithinModule() throws Exception {
    File bundleFile = TestUtils.getTestDataFile(APP_BUNDLE);
    try (ZipFile zipFile = new ZipFile(bundleFile)) {
      ZipEntry entry = zipFile.getEntry("base/manifest/AndroidManifest.xml");
      ZipFileData data = new ZipFileData(zipFile, entry);

      assertThat((Object) data.getPathWithinModule())
          .isEqualTo(Paths.get("manifest/AndroidManifest.xml"));
    }
  }

  @Test
  public void givesPathWithinModule_succeedsForNonModuleFile() throws Exception {
    File bundleFile = TestUtils.getTestDataFile(APP_BUNDLE);
    try (ZipFile zipFile = new ZipFile(bundleFile)) {
      ZipEntry entry = zipFile.getEntry("BundleConfig.pb");
      ZipFileData data = new ZipFileData(zipFile, entry);

      assertThat((Object) data.getPathWithinModule()).isEqualTo(Paths.get("BundleConfig.pb"));
    }
  }

  @Test
  public void getsCorrectSize() throws Exception {
    File bundleFile = TestUtils.getTestDataFile(APP_BUNDLE);
    try (ZipFile zipFile = new ZipFile(bundleFile)) {
      ZipEntry entry = zipFile.getEntry("base/manifest/AndroidManifest.xml");
      ZipFileData data = new ZipFileData(zipFile, entry);

      assertThat(data.getSize()).isEqualTo(2099L);
    }
  }

  @Test
  public void getsCorrectCompressedSize() throws Exception {
    File bundleFile = TestUtils.getTestDataFile(APP_BUNDLE);
    try (ZipFile zipFile = new ZipFile(bundleFile)) {
      ZipEntry entry = zipFile.getEntry("base/manifest/AndroidManifest.xml");
      ZipFileData data = new ZipFileData(zipFile, entry);

      assertThat(data.getCompressedSize()).isEqualTo(794L);
    }
  }
}
