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

import static com.google.common.base.Preconditions.checkState;

import com.google.errorprone.annotations.MustBeClosed;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Supplies the file data for a particular entry in a zipFile. This is not threadsafe. */
public final class ZipFileData implements FileData {

  private final ZipFile zipFile;
  private final ZipEntry entry;
  private InputStream inputStream;
  private Path cachedPathWithinModule;

  public ZipFileData(ZipFile zipFile, ZipEntry entry) {
    this.zipFile = zipFile;
    this.entry = entry;
  }

  /** Returns the input stream for this zipFile. */
  @Override
  @MustBeClosed
  public InputStream getInputStream() {
    checkState(inputStream == null, "input stream was already supplied and opened");

    try {
      inputStream = zipFile.getInputStream(entry);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return inputStream;
  }

  /** Returns the path for this file based on the root of the project/bundle/apk. */
  @Override
  public Path getPathWithinRoot() {
    return Paths.get(entry.getName());
  }

  @Override
  public Path getPathWithinModule() {
    if (cachedPathWithinModule != null) {
      return cachedPathWithinModule;
    }

    Path fullPath = getPathWithinRoot();
    if (fullPath.getNameCount() <= 1
        || fullPath.startsWith("BUNDLE-METADATA")
        || fullPath.startsWith("META-INF")) {
      cachedPathWithinModule = fullPath;
    } else {
      cachedPathWithinModule = fullPath.subpath(1, fullPath.getNameCount());
    }
    return cachedPathWithinModule;
  }

  /** Returns the uncompressed size of this zip entry. */
  @Override
  public long getSize() {
    return entry.getSize();
  }

  /** Returns the compressed size of this zip entry. */
  public long getCompressedSize() {
    return entry.getCompressedSize();
  }
}
