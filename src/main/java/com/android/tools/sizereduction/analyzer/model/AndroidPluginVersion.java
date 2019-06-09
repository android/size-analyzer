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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/** The Android plugin version used by the project. It parses the major and minor version. */
@AutoValue
public abstract class AndroidPluginVersion implements Comparable<AndroidPluginVersion> {

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\..*");
  /**
   * Creates the AndroidPluginVersion. Returns a null object if the raw version is not parseable,
   * such as if a variable is used for the version.
   */
  @Nullable
  public static AndroidPluginVersion create(String rawVersion) {
    Matcher versionMatcher = VERSION_PATTERN.matcher(rawVersion);
    if (versionMatcher.matches()) {
      return new AutoValue_AndroidPluginVersion.Builder()
          .setMajorVersion(Integer.parseInt(versionMatcher.group(1)))
          .setMinorVersion(Integer.parseInt(versionMatcher.group(2)))
          .build();
    }
    return null;
  }

  /** Gets the major version. */
  public abstract int getMajorVersion();

  /** Gets the major version. */
  public abstract int getMinorVersion();

  public int compareTo(AndroidPluginVersion other) {
    // Compare major version first.
    int comparison = compareVersion(getMajorVersion(), other.getMajorVersion());
    if (comparison != 0) {
      return comparison;
    }

    // And then check minor version if major version is the same.
    return compareVersion(getMinorVersion(), other.getMinorVersion());
  }

  private int compareVersion(int a, int b) {
    return a == b ? 0 : (a < b ? -1 : 1);
  }

  /** Builder for the {@link AndroidPluginVersion}. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Set the major version. */
    public abstract Builder setMajorVersion(int majorVersion);

    /** Set the minor version. */
    public abstract Builder setMinorVersion(int minorVersion);

    /** Build the AndroidPluginVersion object. */
    public abstract AndroidPluginVersion build();
  }
}
