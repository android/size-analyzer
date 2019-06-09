/*
 * Copyright (C) 2019 The Android Open Source Project
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class AndroidPluginVersionTest {

  @Test
  public void compareToEqualVersion() throws Exception {
    AndroidPluginVersion version = AndroidPluginVersion.create("3.4.1");
    AndroidPluginVersion version2 = AndroidPluginVersion.create("3.4.0");
    assertThat(version.compareTo(version2)).isEqualTo(0);
  }

  @Test
  public void compareToSmallerMinorVersion() throws Exception {
    AndroidPluginVersion version = AndroidPluginVersion.create("3.3.1");
    AndroidPluginVersion version2 = AndroidPluginVersion.create("3.4.0");
    assertThat(version.compareTo(version2)).isEqualTo(-1);
  }

  @Test
  public void compareToSmallerMajorVersion() throws Exception {
    AndroidPluginVersion version = AndroidPluginVersion.create("2.6.1");
    AndroidPluginVersion version2 = AndroidPluginVersion.create("3.4.0");
    assertThat(version.compareTo(version2)).isEqualTo(-1);
  }

  @Test
  public void compareToLargerMajorVersion() throws Exception {
    AndroidPluginVersion version = AndroidPluginVersion.create("3.8.1");
    AndroidPluginVersion version2 = AndroidPluginVersion.create("2.9.0");
    assertThat(version.compareTo(version2)).isEqualTo(1);
  }
}
