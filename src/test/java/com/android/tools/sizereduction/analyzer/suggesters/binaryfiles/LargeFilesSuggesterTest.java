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

import static com.google.common.truth.Truth.assertThat;

import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.model.FileData;
import com.android.tools.sizereduction.analyzer.model.GradleContext;
import com.android.tools.sizereduction.analyzer.model.testing.FakeFileData;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LargeFilesSuggesterTest {
  private static final String PROJECT_BIN_FILE = "src/main/assets/hello.bin";
  private static final String BIN_FILE = "assets/hello.bin";
  private static final String MP4_VIDEO = "assets/video.mp4";
  private static final String DEX_FILE = "dex/classes.dex";
  private static final String JAVA_FILE = "src/main/java/Main.java";
  private static final long MEDIA_FILE_SIZE = 1024L * 11;
  private static final long LARGE_FILE_SIZE = 1024L * 20;
  private static final long SMALL_FILE_SIZE = 1024L;

  private FileData fileData;

  @Test
  public void smallFile_containsNoSuggestions() {
    fileData = FakeFileData.builder(BIN_FILE).setSize(SMALL_FILE_SIZE).build();
    assertThat(
            new LargeFilesSuggester()
                .processBundleZipEntry(BundleContext.create(1, false), fileData))
        .isEmpty();
  }

  @Test
  public void ignoresNonAssetFiles() {
    fileData = FakeFileData.builder(DEX_FILE).setSize(LARGE_FILE_SIZE).build();
    assertThat(
            new LargeFilesSuggester()
                .processBundleZipEntry(BundleContext.create(1, false), fileData))
        .isEmpty();
  }

  @Test
  public void ignoresProjectNonAssetFiles() {
    fileData = FakeFileData.builder(JAVA_FILE).setSize(LARGE_FILE_SIZE).build();
    assertThat(
            new LargeFilesSuggester()
                .processBundleZipEntry(BundleContext.create(1, false), fileData))
        .isEmpty();
  }

  @Test
  public void isOnDemandModule_noSuggestion() {
    fileData = FakeFileData.builder(MP4_VIDEO).build();
    assertThat(
            new LargeFilesSuggester()
                .processBundleZipEntry(BundleContext.create(1, true), fileData))
        .isEmpty();
  }

  @Test
  public void notOnDemandModule_containsSuggestions() {
    fileData = FakeFileData.builder(MP4_VIDEO).setSize(MEDIA_FILE_SIZE).build();
    List<Suggestion> suggestions =
        new LargeFilesSuggester().processBundleZipEntry(BundleContext.create(1, false), fileData);
    assertThat(suggestions).hasSize(2);
    assertThat(suggestions)
        .containsExactly(
            Suggestion.create(
                Suggestion.IssueType.MEDIA_STREAMING,
                Suggestion.Category.LARGE_FILES,
                Payload.getDefaultInstance(),
                "Stream media file " + MP4_VIDEO + " from the internet to avoid bundling in apk",
                MEDIA_FILE_SIZE,
                /* autoFix= */ null),
            Suggestion.create(
                Suggestion.IssueType.LARGE_FILES_DYNAMIC_FEATURE,
                Suggestion.Category.LARGE_FILES,
                Payload.getDefaultInstance(),
                "Place large file "
                    + MP4_VIDEO
                    + " inside an on demand dynamic-feature to avoid bundling in apk",
                MEDIA_FILE_SIZE,
                /* autoFix= */ null));
  }

  @Test
  public void largeFile_containsOnlyLargeFileSuggestion() {
    fileData = FakeFileData.builder(BIN_FILE).setSize(LARGE_FILE_SIZE).build();
    List<Suggestion> suggestions =
        new LargeFilesSuggester().processBundleZipEntry(BundleContext.create(1, false), fileData);
    assertThat(suggestions).hasSize(1);
    assertThat(suggestions)
        .containsExactly(
            Suggestion.create(
                Suggestion.IssueType.LARGE_FILES_DYNAMIC_FEATURE,
                Suggestion.Category.LARGE_FILES,
                Payload.getDefaultInstance(),
                "Place large file "
                    + BIN_FILE
                    + " inside an on demand dynamic-feature to avoid bundling in apk",
                LARGE_FILE_SIZE,
                /* autoFix= */ null));
  }

  @Test
  public void projectLargeFile_containsOnlyLargeFileSuggestion() {
    fileData = FakeFileData.builder(PROJECT_BIN_FILE).setSize(LARGE_FILE_SIZE).build();
    List<Suggestion> suggestions =
        new LargeFilesSuggester().processProjectEntry(GradleContext.create(1, false), fileData);
    assertThat(suggestions).hasSize(1);
    assertThat(suggestions)
        .containsExactly(
            Suggestion.create(
                Suggestion.IssueType.LARGE_FILES_DYNAMIC_FEATURE,
                Suggestion.Category.LARGE_FILES,
                Payload.getDefaultInstance(),
                "Place large file "
                    + PROJECT_BIN_FILE
                    + " inside an on demand dynamic-feature to avoid bundling in apk",
                LARGE_FILE_SIZE,
                /* autoFix= */ null));
  }
}
