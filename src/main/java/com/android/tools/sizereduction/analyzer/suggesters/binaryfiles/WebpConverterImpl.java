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

import com.google.webp.libwebp;
import java.awt.image.BufferedImage;

/** Converts images to webp format. */
public final class WebpConverterImpl implements WebpConverter {

  @Override
  public byte[] encodeLosslessWebp(BufferedImage image) {
    WebpNativeLibLoader.loadNativeLibraryIfNeeded();

    // Build BGRA array as expected by libwebp.
    byte[] bgraArray = new byte[image.getWidth() * image.getHeight() * 4];
    int i = 0;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        int argb = image.getRGB(x, y); // 4 bytes in ARGB order.
        // Put the ARGB values in reverse order into the BGRA array.
        for (int b = 0; b < 4; b++) {
          bgraArray[i++] = (byte) argb;
          argb >>= 8;
        }
      }
    }

    return libwebp.WebPEncodeLosslessBGRA(
        bgraArray, image.getWidth(), image.getHeight(), image.getWidth() * 4);
  }
}
