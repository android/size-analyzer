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

package com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.testing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.WebpConverter;
import java.awt.image.BufferedImage;

/** Fake implementation of Webp converter which simply returns bytes provided to it. */
public final class FakeWebpConverter implements WebpConverter {

  private byte[] fakeData;

  @Override
  public byte[] encodeLosslessWebp(BufferedImage image) {
    return checkNotNull(fakeData, "setFakeData() was not called.");
  }

  public void setFakeData(byte[] data) {
    fakeData = checkNotNull(data);
  }
}
