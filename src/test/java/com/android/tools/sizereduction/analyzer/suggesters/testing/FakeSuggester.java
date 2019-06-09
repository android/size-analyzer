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

package com.android.tools.sizereduction.analyzer.suggesters.testing;

import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.model.Context;
import com.android.tools.sizereduction.analyzer.model.FileData;
import com.android.tools.sizereduction.analyzer.model.GradleContext;
import com.android.tools.sizereduction.analyzer.suggesters.ApkEntrySuggester;
import com.android.tools.sizereduction.analyzer.suggesters.ApkSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.BundleEntrySuggester;
import com.android.tools.sizereduction.analyzer.suggesters.BundleSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.ProjectSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.ProjectTreeSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;

/**
 * Generic fake suggester implementation that should be used for testing in place of all kinds of
 * suggesters, be it APK, Bundle, or any other suggester.
 *
 * <p>The fake suggester accepts the suggestions it should later return when called by an analyzer.
 * The suggestions are stored either in a keyed multimap which is used to look up suggestions for
 * entry-organized objects the suggester scans, or in a list which is returned when the suggester is
 * asked to scan an artifact as a whole (in which case there is really no entry organization).
 */
public final class FakeSuggester
    implements ApkSuggester,
        BundleSuggester,
        ApkEntrySuggester,
        BundleEntrySuggester,
        ProjectSuggester,
        ProjectTreeSuggester {

  // Record of analyzed entries in case when the suggester scans an entry-organized artifact, on an
  // entry level,
  private final Set<String> analyzedEntries = new HashSet<>();

  // List of suggestions that should be returned when suggestions are requested for the whole
  // artifact.
  @Nullable private ImmutableList<Suggestion> artifactSuggestions;

  // Map containing suggestions keyed by context and entry paths that should be returned when
  // entries are requested for a particular context/entry.
  @Nullable private ImmutableMultimap<ContextAndEntryPath, Suggestion> entrySuggestions;

  @Override
  public ImmutableList<Suggestion> processApk(Context context, ZipFile apk) {
    return getArtifactSuggestions();
  }

  @Override
  public ImmutableList<Suggestion> processBundle(
      BundleContext context, AppBundle bundle, ZipFile bundleZip) {
    return getArtifactSuggestions();
  }

  @Override
  public ImmutableList<Suggestion> processProject(GradleContext context, File projectDir) {
    return getArtifactSuggestions();
  }

  @Override
  public ImmutableList<Suggestion> processApkZipEntry(Context context, FileData entry) {
    analyzedEntries.add(entry.getPathWithinRoot().toString());
    return getEntrySuggestions(context, entry);
  }

  @Override
  public ImmutableList<Suggestion> processBundleZipEntry(BundleContext context, FileData entry) {
    analyzedEntries.add(entry.getPathWithinRoot().toString());
    return getEntrySuggestions(context, entry);
  }

  @Override
  public ImmutableList<Suggestion> processProjectEntry(GradleContext context, FileData entry) {
    analyzedEntries.add(entry.getPathWithinRoot().toString());
    return getEntrySuggestions(context, entry);
  }

  public void setArtifactSuggestions(ImmutableList<Suggestion> suggestions) {
    artifactSuggestions = suggestions;
  }

  public void setEntrySuggestions(ImmutableMultimap<ContextAndEntryPath, Suggestion> suggestions) {
    entrySuggestions = suggestions;
  }

  /** Returns the set of files visited by this analyzer. */
  public ImmutableSet<String> getAnalyzedEntries() {
    return ImmutableSet.copyOf(analyzedEntries);
  }

  private ImmutableList<Suggestion> getArtifactSuggestions() {
    if (artifactSuggestions == null) {
      return ImmutableList.of();
    }

    return artifactSuggestions;
  }

  private ImmutableList<Suggestion> getEntrySuggestions(Context context, FileData entry) {
    if (entrySuggestions == null) {
      return ImmutableList.of();
    }

    return ImmutableList.copyOf(
        entrySuggestions.get(
            ContextAndEntryPath.create(context, entry.getPathWithinRoot().toString())));
  }

  /**
   * Contains the pair of context and an entry path used to provide instructions to the {@code
   * FakeSuggester} on how to provide suggestions.
   */
  @AutoValue
  public abstract static class ContextAndEntryPath {

    public static ContextAndEntryPath create(Context context, String entryPath) {
      return new AutoValue_FakeSuggester_ContextAndEntryPath(context, entryPath);
    }

    public abstract Context context();

    public abstract String entryPath();
  }
}
