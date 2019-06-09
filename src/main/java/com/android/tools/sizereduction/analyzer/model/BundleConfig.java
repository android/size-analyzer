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
 * limitations under the License
 */

package com.android.tools.sizereduction.analyzer.model;

import com.google.auto.value.AutoValue;

/** Information regarding the bundle configuration set in the build.gradle */
@AutoValue
public abstract class BundleConfig {

  /** Whether this language splits are enabled. */
  public abstract boolean getLanguageSplitEnabled();

  /** Whether this density splits are enabled. */
  public abstract boolean getDensitySplitEnabled();

  /** Whether this abi splits are enabled. */
  public abstract boolean getAbiSplitEnabled();

  public static Builder builder() {
    // by default, all the splits are enabled.
    return new AutoValue_BundleConfig.Builder()
        .setLanguageSplitEnabled(true)
        .setDensitySplitEnabled(true)
        .setAbiSplitEnabled(true);
  }

  /** Builder for the {@link BundleConfig}. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Set whether the language splits are enabled. */
    public abstract Builder setLanguageSplitEnabled(boolean languageSplitEnabled);

    /** Set whether the density splits are enabled. */
    public abstract Builder setDensitySplitEnabled(boolean densitySplitEnabled);

    /** Set whether the abi splits are enabled. */
    public abstract Builder setAbiSplitEnabled(boolean abiSplitEnabled);

    /** Build the bundle config object. */
    public abstract BundleConfig build();
  }
}
