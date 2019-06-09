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

package com.android.tools.sizereduction.analyzer.suggesters.binaryfiles;

import static com.android.tools.sizereduction.analyzer.utils.TestUtils.getTestDataFile;
import static com.google.common.truth.Truth.assertThat;

import com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.testing.FakeWebpConverter;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;
import java.io.File;
import java.nio.file.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class WebpAutoFixTest {

  @Test
  public void appliesFix() throws Exception {
    FakeWebpConverter fakeConverter = new FakeWebpConverter();
    byte[] bytes = new byte[] {0, 1, 2, 4, 8};
    fakeConverter.setFakeData(bytes);
    File pngFile = File.createTempFile("foo", ".png");
    Files.copy(getTestDataFile("webp/drawing.png"), pngFile);
    Path pngPath = pngFile.toPath();
    new WebpAutoFix(pngPath, fakeConverter).apply();

    File webpFile =
        new File(
            pngPath
                .resolveSibling(MoreFiles.getNameWithoutExtension(pngPath) + ".webp")
                .toString());
    assertThat(pngFile.exists()).isFalse();
    assertThat(webpFile.exists()).isTrue();
    assertThat(Files.asByteSource(webpFile).read()).isEqualTo(bytes);
    webpFile.delete();
  }
}
