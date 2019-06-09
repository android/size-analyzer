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

package com.android.tools.sizereduction.analyzer.model.testing;

import com.android.tools.sizereduction.analyzer.model.FileData;
import com.google.auto.value.AutoValue;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Fake implementation of {@link FileData} that can be filled up with dummy data that will be
 * returned by the getter methods.
 */
@AutoValue
public abstract class FakeFileData implements FileData {

  /** Returns a builder with a default dummy input stream and size. */
  public static Builder builder() {
    return new AutoValue_FakeFileData.Builder()
        .setInputStream(new ByteArrayInputStream(new byte[0]))
        .setSize(1L);
  }

  /**
   * Returns a FakeFileData with the given path as the module and root path with a default dummy
   * input stream and size.
   */
  public static Builder builder(String dummyPath) {
    Path bothPaths = Paths.get(dummyPath);
    return builder().setPathWithinRoot(bothPaths).setPathWithinModule(bothPaths);
  }

  @Override
  public abstract InputStream getInputStream();

  @Override
  public abstract Path getPathWithinRoot();

  @Override
  public abstract Path getPathWithinModule();

  @Override
  public abstract long getSize();

  /** Builder for the FakeDileData. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setInputStream(InputStream inputStream);

    public abstract Builder setPathWithinRoot(Path pathWithinRoot);

    public abstract Builder setPathWithinModule(Path pathWithinModule);

    public abstract Builder setSize(long size);

    public abstract FakeFileData build();
  }
}
