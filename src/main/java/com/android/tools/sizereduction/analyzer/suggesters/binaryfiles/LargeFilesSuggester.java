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

import static com.android.tools.sizereduction.analyzer.model.FileData.getFileExtension;

import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.model.Context;
import com.android.tools.sizereduction.analyzer.model.FileData;
import com.android.tools.sizereduction.analyzer.model.GradleContext;
import com.android.tools.sizereduction.analyzer.model.ZipFileData;
import com.android.tools.sizereduction.analyzer.suggesters.BundleEntrySuggester;
import com.android.tools.sizereduction.analyzer.suggesters.ProjectTreeSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Suggests media files that can be streamed or large files that can be placed in an on-demand
 * dynamic-feature.
 */
public class LargeFilesSuggester implements BundleEntrySuggester, ProjectTreeSuggester {

  private static final ImmutableSet<String> MEDIA_FILE_TYPES =
      ImmutableSet.of(
          "mp4", "m4p", "m4v", "mpg", "mp2", "mpeg", "mpe", "mpv", "m2v", "vob", "rm", "mp3", "3gp",
          "aa", "aac", "wav", "flac", "m4a", "mpc", "mmf", "wma", "wv");
  private static final ImmutableList<Pattern> BUNDLE_ASSET_FILES =
      ImmutableList.of(Pattern.compile("res/.*"), Pattern.compile("assets/.*"));
  private static final ImmutableList<Pattern> PROJECT_ASSET_FILES =
      ImmutableList.of(
          Pattern.compile("src/main/res/.*"),
          Pattern.compile("src/main/resources/.*"),
          Pattern.compile("src/main/assets/.*"));
  // 10 KB files or under don't really need to be looked at.
  private static final long SMALL_FILE_SIZE_LIMIT = 1024 * 10;

  public LargeFilesSuggester() {}

  @Override
  public ImmutableList<Suggestion> processBundleZipEntry(BundleContext context, FileData fileData) {
    return processFileEntry(context, fileData, isBundleAssetFile(fileData));
  }

  @Override
  public ImmutableList<Suggestion> processProjectEntry(GradleContext context, FileData fileData) {
    return processFileEntry(context, fileData, isProjectAssetFile(fileData));
  }

  private ImmutableList<Suggestion> processFileEntry(
      Context context, FileData fileData, boolean isAssetFile) {
    if (context.getOnDemand() || fileData.getSize() < SMALL_FILE_SIZE_LIMIT) {
      return ImmutableList.of();
    }

    // Here, we make the assumption that media formats don't get much smaller when the file is
    // zipped, so we use the uncompressed file size.
    final boolean isMediaFile = MEDIA_FILE_TYPES.contains(getFileExtension(fileData));
    final long size =
        (isMediaFile && fileData instanceof ZipFileData)
            ? ((ZipFileData) fileData).getCompressedSize()
            : fileData.getSize();

    ImmutableList.Builder<Suggestion> suggestions = ImmutableList.<Suggestion>builder();
    if (isAssetFile || isMediaFile) {
      suggestions.add(
          Suggestion.create(
              Suggestion.IssueType.LARGE_FILES_DYNAMIC_FEATURE,
              Suggestion.Category.LARGE_FILES,
              Payload.getDefaultInstance(),
              "Place large file "
                  + fileData.getPathWithinRoot()
                  + " inside an on demand dynamic-feature to avoid bundling in apk",
              size,
              /* autoFix= */ null));
    }
    if (isMediaFile) {
      suggestions.add(
          Suggestion.create(
              Suggestion.IssueType.MEDIA_STREAMING,
              Suggestion.Category.LARGE_FILES,
              Payload.getDefaultInstance(),
              "Stream media file "
                  + fileData.getPathWithinRoot()
                  + " from the internet to avoid bundling in apk",
              size,
              /* autoFix= */ null));
    }
    return suggestions.build();
  }

  private static boolean isBundleAssetFile(FileData fileData) {
    Path path = fileData.getPathWithinModule();
    return BUNDLE_ASSET_FILES.stream()
        .map(pattern -> pattern.matcher(path.toString()))
        .anyMatch(matcher -> matcher.matches());
  }

  private static boolean isProjectAssetFile(FileData fileData) {
    Path path = fileData.getPathWithinModule();
    return PROJECT_ASSET_FILES.stream()
        .map(pattern -> pattern.matcher(path.toString()))
        .anyMatch(matcher -> matcher.matches());
  }
}
