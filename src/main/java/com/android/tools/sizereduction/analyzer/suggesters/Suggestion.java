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

package com.android.tools.sizereduction.analyzer.suggesters;

import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** Represents a single suggestion for reducing the size of an app. */
@AutoValue
public abstract class Suggestion {
  /** The suggestion category for a given suggestion. */
  public enum Category {
    WEBP,
    LARGE_FILES,
    PROGUARD,
    BUNDLE_CONFIG,
  }

  /** The specific issue type for a given suggestion. */
  public enum IssueType {
    WEBP,
    MEDIA_STREAMING,
    LARGE_FILES_DYNAMIC_FEATURE,
    PROGUARD_NO_MAP,
    PROGUARD_EMPTY_MAP,
    PROGUARD_NO_SHRINKING,
    PROGUARD_NO_OBFUSCATION,
    QUESTIONABLE_FILE,
    BUNDLES_NO_ABI_SPLITTING,
    BUNDLES_NO_DENSITY_SPLITTING,
    BUNDLES_NO_LANGUAGE_SPLITTING,
    BUNDLES_NO_UNCOMPRESSED_NATIVE_LIBS,
  }

  public static Suggestion create(
      IssueType issueType,
      Category category,
      Payload payload,
      String message,
      @Nullable Long estimatedBytesSaved,
      @Nullable AutoFix autoFix) {
    return new AutoValue_Suggestion(
        issueType, category, payload, message, estimatedBytesSaved, autoFix);
  }

  public abstract IssueType getIssueType();

  public abstract Category getCategory();

  public abstract Payload payload();

  public abstract String getMessage();

  @Nullable
  public abstract Long getEstimatedBytesSaved();

  @Nullable
  public abstract AutoFix getAutoFix();

  @Override
  public final String toString() {
    Long bytesSaved = getEstimatedBytesSaved();
    return getMessage() + (bytesSaved == null ? "" : " (saves " + bytesSaved + " bytes)");
  }
}
