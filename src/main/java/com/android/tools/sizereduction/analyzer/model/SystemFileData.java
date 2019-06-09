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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/** Supplies the file data for a file on the file system. This is not threadsafe. */
public final class SystemFileData implements FileData {

  private final File file;
  private final Path pathWithinRoot;
  private final Path pathWithinModule;
  private InputStream inputStream;

  public SystemFileData(File file, Path pathWithinRoot) {
    this(file, pathWithinRoot, pathWithinRoot);
  }

  public SystemFileData(File file, Path pathWithinRoot, Path pathWithinModule) {
    this.file = file;
    this.pathWithinRoot = pathWithinRoot;
    this.pathWithinModule = pathWithinModule;
  }

  /** Returns the input stream for this file. */
  @Override
  @MustBeClosed
  public InputStream getInputStream() {
    checkState(inputStream == null, "input stream was already supplied and opened");

    try {
      inputStream = new FileInputStream(file);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return inputStream;
  }

  /** Returns the path for this file based on the root of the project/bundle/apk. */
  @Override
  public Path getPathWithinRoot() {
    return pathWithinRoot;
  }

  @Override
  public Path getPathWithinModule() {
    return pathWithinModule;
  }

  public Path getSystemPath() {
    return file.toPath();
  }

  /** Returns the size of this file. */
  @Override
  public long getSize() {
    return file.length();
  }
}
