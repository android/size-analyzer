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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class WebpConverterImplTest {

  @Before
  public void setUp() {
    WebpNativeLibLoader.setDiskLocation(
        new File(Paths.get("").toFile().getAbsolutePath() + "/libs/libwebp"));
  }

  @Test
  public void encodeLosslessWebp() throws IOException {
    BufferedImage inputImage = ImageIO.read(getTestDataFile("webp/drawing.png"));
    byte[] expectedOutputBytes =
        Files.readAllBytes(getTestDataFile("webp/drawing_q70.webp").toPath());

    byte[] outputBytes = new WebpConverterImpl().encodeLosslessWebp(inputImage);

    assertThat(outputBytes).isEqualTo(expectedOutputBytes);
  }
}
