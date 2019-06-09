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

import com.google.common.base.Ascii;
import com.google.common.io.MoreFiles;
import com.google.errorprone.annotations.MustBeClosed;
import java.io.InputStream;
import java.nio.file.Path;

/** This class contains the information for a given file to be analyzed by a suggester. */
public interface FileData {

  /** Returns the input stream for this file. */
  @MustBeClosed
  InputStream getInputStream();

  /** Returns the path for this file based on the root of the project/bundle/apk. */
  Path getPathWithinRoot();

  /** Returns the path for this file based on the current module of the project/bundle/apk. */
  Path getPathWithinModule();

  /** Returns the size of this file. */
  long getSize();

  /** Returns the file's lowercase extension without the dot, or an empty string if no extension. */
  static String getFileExtension(FileData fileData) {
    return Ascii.toLowerCase(MoreFiles.getFileExtension(fileData.getPathWithinRoot()));
  }
}
