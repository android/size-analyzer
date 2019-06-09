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
import com.google.common.annotations.VisibleForTesting;

/** Context for the bundle being analyzed, to be used by the suggester when it is called. */
@AutoValue
public abstract class BundleContext implements Context {

  public static BundleContext create(int minSdkVersion, boolean onDemand) {
    return new AutoValue_BundleContext.Builder()
        .setMinSdkVersion(minSdkVersion)
        .setOnDemand(onDemand)
        .build();
  }

  @VisibleForTesting
  public static BundleContext create(int minSdkVersion) {
    return new AutoValue_BundleContext.Builder()
        .setMinSdkVersion(minSdkVersion)
        .setOnDemand(false)
        .build();
  }

  /** The min sdk version declared for this project. */
  @Override
  public abstract int getMinSdkVersion();

  /** The on demand value declared for this project. */
  @Override
  public abstract boolean getOnDemand();

  /** Builder for the {@link BundleContext}. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Set the min sdk version. */
    public abstract Builder setMinSdkVersion(int minSdkVersion);

    /** Set whether the bundle module is an onDemand module. */
    public abstract Builder setOnDemand(boolean onDemand);

    /** Build the context object. */
    public abstract BundleContext build();
  }
}
