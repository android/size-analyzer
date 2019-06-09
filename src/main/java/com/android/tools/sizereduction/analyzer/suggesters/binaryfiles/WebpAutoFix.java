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

import com.android.tools.sizereduction.analyzer.suggesters.AutoFix;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.CountingInputStream;
import com.google.common.io.MoreFiles;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Converts an image into a lossless webp image. */
public final class WebpAutoFix implements AutoFix {

  private Path filePath;
  private WebpConverter webpConverter;

  public WebpAutoFix(Path filePath) {
    this(filePath, new WebpConverterImpl());
  }

  @VisibleForTesting
  WebpAutoFix(Path filePath, WebpConverter webpConverter) {
    this.filePath = filePath;
    this.webpConverter = webpConverter;
  }

  /**
   * Writes the provided bytes into a new file path, based on the initial image's filepath, but with
   * a .webp extension.
   */
  public void apply() {
    Path newFilePath =
        filePath.resolveSibling(MoreFiles.getNameWithoutExtension(filePath) + ".webp");
    try (InputStream inputStream = new FileInputStream(new File(filePath.toString()))) {
      CountingInputStream countingStream = new CountingInputStream(inputStream);
      BufferedImage bufferedImage = WebpSuggester.safelyParseImage(countingStream);

      long oldSize = countingStream.getCount();
      byte[] webpBytes = webpConverter.encodeLosslessWebp(bufferedImage);
      Files.write(newFilePath, webpBytes);
      Files.delete(filePath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (!(other instanceof WebpAutoFix)) {
      return false;
    }
    WebpAutoFix autoFix = (WebpAutoFix) other;
    return Objects.equals(this.filePath, autoFix.filePath);
  }

  public int hashCode() {
    return Objects.hash(filePath);
  }
}
