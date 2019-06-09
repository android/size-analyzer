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
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;

/** Context for the gradle project being analyzed, to be used by the suggester when it is called. */
@AutoValue
public abstract class GradleContext implements Context {

  /** Plugin type for the current gradle project or subproject. */
  public enum PluginType {
    UNKNOWN,
    APPLICATION,
    DYNAMIC_FEATURE,
    FEATURE,
  };

  public static GradleContext create(int minSdkVersion, boolean onDemand) {
    return builder().setMinSdkVersion(minSdkVersion).setOnDemand(onDemand).build();
  }

  public static GradleContext create(int minSdkVersion) {
    return builder().setMinSdkVersion(minSdkVersion).build();
  }

  public static Builder builder() {
    // set obfuscation enabled to true, as that is the default value.
    return new AutoValue_GradleContext.Builder()
        .setOnDemand(false)
        .setProguardConfigs(ImmutableMap.of())
        .setPluginType(PluginType.UNKNOWN)
        .setBundleConfig(BundleConfig.builder().build());
  }

  /** The min sdk version declared for this project. */
  @Override
  public abstract int getMinSdkVersion();

  /** The on demand value declared for this project. */
  @Override
  public abstract boolean getOnDemand();

  /** Get the plugin type for this project. */
  public abstract PluginType getPluginType();

  /** Get the gradle build version for this project. */
  @Nullable
  public abstract AndroidPluginVersion getAndroidPluginVersion();

  /** Gets the proguard configurations. */
  public abstract ImmutableMap<String, ProguardConfig> getProguardConfigs();

  /** Gets the Bundle Configuration for the gradle project. */
  public abstract BundleConfig getBundleConfig();

  /** Builder for the {@link GradleContext}. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Set the min sdk version. */
    public abstract Builder setMinSdkVersion(int minSdkVersion);

    /** Set whether the project/module is an onDemand module. */
    public abstract Builder setOnDemand(boolean onDemand);

    /** Set the plugin type for this project. */
    public abstract Builder setPluginType(PluginType pluginType);

    /** Set the Android gradle plugin version. */
    public abstract Builder setAndroidPluginVersion(@Nullable AndroidPluginVersion pluginVersion);

    /** Sets the proguard configurations. */
    public abstract Builder setProguardConfigs(
        ImmutableMap<String, ProguardConfig> proguardConfigs);

    /** Set the Bundle Configuration for the gradle project. */
    public abstract Builder setBundleConfig(BundleConfig pluginVersion);

    /** Build the context object. */
    public abstract GradleContext build();
  }
}
