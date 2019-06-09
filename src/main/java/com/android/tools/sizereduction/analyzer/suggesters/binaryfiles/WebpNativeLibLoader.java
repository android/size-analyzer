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
 * limitations under the License.
 */

package com.android.tools.sizereduction.analyzer.suggesters.binaryfiles;

import com.android.tools.sizereduction.analyzer.model.SystemInformation;
import com.google.common.annotations.VisibleForTesting;
import com.google.webp.libwebp;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/** Loads the appropriate webp native library for the platform if it hasn't already been loaded. */
public class WebpNativeLibLoader {

  private static boolean jniLibLoaded = false;
  private static boolean jniLibLoadAttempted = false;
  private static File dirLocation;

  private WebpNativeLibLoader() {}

  /**
   * Set the disk location directly for where to load the libwebp native libraries. This is used
   * primarily for testing when the executable jar is not used.
   */
  @VisibleForTesting
  static synchronized void setDiskLocation(File dirLocation) {
    WebpNativeLibLoader.dirLocation = dirLocation;
  }

  public static synchronized boolean loadNativeLibraryIfNeeded() {
    if (!jniLibLoadAttempted) {
      try {
        if (tryDefaultLoad()) {
          return jniLibLoaded;
        }
        if (dirLocation != null) {
          System.load(
              dirLocation.getAbsolutePath() + File.separatorChar + getFolderPath() + getLibName());
        } else {
          loadNativeLibraryFromJar();
        }
      } catch (UnsatisfiedLinkError e) {
        throw new RuntimeException(e);
      }
    }
    return jniLibLoaded;
  }

  private static synchronized boolean tryDefaultLoad() {
    try {
      libwebp.WebPGetDecoderVersion();
      jniLibLoadAttempted = true;
      jniLibLoaded = true;
      return true;
    } catch (UnsatisfiedLinkError e) {
      return false;
    }
  }

  private static synchronized void loadNativeLibraryFromJar() {
    if (jniLibLoadAttempted) {
      // Already attempted to load, nothing to do here.
      return;
    }
    try {
      String libFileName = getLibName();
      InputStream lib =
          WebpNativeLibLoader.class
              .getClassLoader()
              .getResourceAsStream(getFolderPath() + libFileName);
      File libFile = File.createTempFile(libFileName, ".tmp");
      Files.copy(lib, libFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      System.load(libFile.getAbsolutePath());
      libFile.deleteOnExit();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      jniLibLoadAttempted = true;
    }
    jniLibLoaded = true;
  }

  private static String getLibName() {
    String baseName = SystemInformation.is32Bit() ? "libwebp_jni" : "libwebp_jni64";
    String extension =
        SystemInformation.isWindows() ? ".dll" : SystemInformation.isMac() ? ".dylib" : ".so";
    return baseName + extension;
  }

  private static String getFolderPath() {
    String folder =
        SystemInformation.isWindows() ? "win/" : SystemInformation.isMac() ? "mac/" : "linux/";
    return folder;
  }
}
