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

package com.android.tools.sizereduction.analyzer.suggesters.binaryfiles;

import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.model.FileData;
import com.android.tools.sizereduction.analyzer.model.GradleContext;
import com.android.tools.sizereduction.analyzer.model.ZipFileData;
import com.android.tools.sizereduction.analyzer.suggesters.BundleEntrySuggester;
import com.android.tools.sizereduction.analyzer.suggesters.ProjectTreeSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Suggests certain files that may not be able to be removed, and are unncessary for your
 * application to function.
 */
public class QuestionableFilesSuggester implements BundleEntrySuggester, ProjectTreeSuggester {

  public QuestionableFilesSuggester() {}

  // 1 KB files or under don't really need to be looked at.
  private static final long SMALL_FILE_SIZE_LIMIT = 1024;
  private static final ImmutableList<Pattern> STANDARD_BUNDLE_FILES =
      ImmutableList.of(
          Pattern.compile("BundleConfig.pb"),
          Pattern.compile("BUNDLE-METADATA/.*"),
          Pattern.compile("resources.pb"),
          Pattern.compile("assets.pb"),
          Pattern.compile("manifest/AndroidManifest.xml"),
          Pattern.compile("res/.*"),
          Pattern.compile("assets/.*"),
          Pattern.compile("dex/.*\\.dex"),
          Pattern.compile("lib/.*\\.so"),
          Pattern.compile("root/META-INF/CERT.*"),
          Pattern.compile("root/META-INF/KEY.*"),
          Pattern.compile("root/META-INF/MANIFEST.MF"),
          Pattern.compile("META-INF/ANDROIDD.SF*"),
          Pattern.compile("META-INF/ANDROIDD.RSA*"),
          Pattern.compile("META-INF/MANIFEST.MF"));
  private static final ImmutableList<Pattern> STANDARD_PROJECT_FILES =
      ImmutableList.of(
          Pattern.compile("^(?!src/main/).*$"), // match anything that does not start with src/main/
          Pattern.compile("src/main/res/.*"), // res folder is okay
          Pattern.compile("src/main/assets/.*"), // assets folder is okay
          Pattern.compile("src/main/java/.*"), // java folder is okay
          Pattern.compile("src/main/AndroidManifest.xml")); // manifest is okay

  @Override
  public ImmutableList<Suggestion> processBundleZipEntry(BundleContext context, FileData fileData) {
    if (fileData.getSize() < SMALL_FILE_SIZE_LIMIT || isStandardBundleFile(fileData)) {
      return ImmutableList.of();
    }
    return processFileEntry(fileData);
  }

  @Override
  public ImmutableList<Suggestion> processProjectEntry(GradleContext context, FileData fileData) {
    if (fileData.getSize() < SMALL_FILE_SIZE_LIMIT || isStandardProjectFile(fileData)) {
      return ImmutableList.of();
    }
    return processFileEntry(fileData);
  }

  private ImmutableList<Suggestion> processFileEntry(FileData fileData) {
    long savingsEstimate =
        fileData instanceof ZipFileData
            ? ((ZipFileData) fileData).getCompressedSize()
            : fileData.getSize();
    return ImmutableList.of(
        Suggestion.create(
            Suggestion.IssueType.QUESTIONABLE_FILE,
            Suggestion.Category.LARGE_FILES,
            Payload.getDefaultInstance(),
            getSuggestionMessage(fileData.getPathWithinRoot()),
            savingsEstimate,
            /* autoFix= */ null));
  }

  @VisibleForTesting
  static String getSuggestionMessage(Path filePath) {
    return "File "
        + filePath
        + " does not appear to be a needed file. Consider removing this file or placing it in the"
        + " assets directory. If the file is added through a library dependency, consider using"
        + " packagingOptions to exclude the file "
        + "https://google.github.io/android-gradle-dsl/current/"
        + "com.android.build.gradle.internal.dsl.PackagingOptions.html";
  }

  private static boolean isStandardBundleFile(FileData fileData) {
    Path path = fileData.getPathWithinModule();
    return STANDARD_BUNDLE_FILES.stream()
        .map(pattern -> pattern.matcher(path.toString()))
        .anyMatch(matcher -> matcher.matches());
  }

  private static boolean isStandardProjectFile(FileData fileData) {
    Path path = fileData.getPathWithinModule();
    return STANDARD_PROJECT_FILES.stream()
        .map(pattern -> pattern.matcher(path.toString()))
        .anyMatch(matcher -> matcher.matches());
  }
}
