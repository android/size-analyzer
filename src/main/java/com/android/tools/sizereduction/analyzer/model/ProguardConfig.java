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

/** Information regarding the proguard configuration. This may be unique to each build type. */
@AutoValue
public abstract class ProguardConfig {

  public static final String DEFAULT_CONFIG_NAME = "android.default";

  /** Whether this project allows for code shrinking. */
  public abstract boolean getMinifyEnabled();

  /** Whether this project has set any proguard rules for code shrinking/obfuscation. */
  public abstract boolean getHasProguardRules();

  /** Whether this project has obfuscation enabled (tied to whether proguard/r8 is being used). */
  public abstract boolean getObfuscationEnabled();

  public static Builder builder() {
    // set obfuscation enabled to true, as that is the default value.
    return new AutoValue_ProguardConfig.Builder()
        .setObfuscationEnabled(true)
        .setMinifyEnabled(false)
        .setHasProguardRules(false);
  }

  /** Builder for the {@link ProguardConfig}. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Set the min sdk version. */
    public abstract Builder setMinifyEnabled(boolean minifyEnabled);

    /** Set whether the project/module is an onDemand module. */
    public abstract Builder setHasProguardRules(boolean hasProguardRules);

    /** Set whether the project/module is an onDemand module. */
    public abstract Builder setObfuscationEnabled(boolean obfuscationEnabled);

    /** Build the context object. */
    public abstract ProguardConfig build();
  }
}
