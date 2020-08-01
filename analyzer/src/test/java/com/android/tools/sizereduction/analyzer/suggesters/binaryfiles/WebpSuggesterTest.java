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

import static com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.WebpSuggester.MIN_SDK_VERSION_SUPPORTING_LOSSLESS_WEBP;	
import static com.android.tools.sizereduction.analyzer.utils.TestUtils.getTestDataFile;	
import static com.google.common.truth.Truth.assertThat;	

import com.android.tools.sizereduction.analyzer.SuggestionPayload.FileEntryData;	
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;	
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload.WebpData;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.model.FileData;
import com.android.tools.sizereduction.analyzer.model.SystemFileData;
import com.android.tools.sizereduction.analyzer.model.testing.FakeFileData;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.model.GradleContext;	
import com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.testing.FakeWebpConverter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Assume;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static com.android.tools.sizereduction.analyzer.suggesters.binaryfiles.WebpSuggester.MIN_SDK_VERSION_SUPPORTING_LOSSLESS_WEBP;
import static com.android.tools.sizereduction.analyzer.utils.TestUtils.getTestDataFile;
import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class WebpSuggesterTest {
  private static final String PNG_DRAWING = "webp/drawing.png";
  private static final long DRAWING_FILE_SIZE = 54127L;
  private static final String JPG_PHOTO = "webp/photo.jpg";

  @Before
  public void setUp() {
    File f = new File(Paths.get("").toFile().getAbsolutePath() + "/libs/libwebp");
    Assume.assumeTrue(f.exists());
    WebpNativeLibLoader.setDiskLocation(f);
  }

  @Test
  public void notAnImageFile_noSuggestion() {
    FileData file = FakeFileData.builder("hello.ico").build();
    assertThat(
            new WebpSuggester()
                .processBundleZipEntry(
                    BundleContext.create(MIN_SDK_VERSION_SUPPORTING_LOSSLESS_WEBP), file))
        .isEmpty();
  }

  @Test
  public void losslessReducibleDrawing_supportedApiLevel_reducesSize() throws Exception {
    FakeWebpConverter fakeConverter = new FakeWebpConverter();
    WebpSuggester webpSuggester = new WebpSuggester(fakeConverter);
    int webpSize = (int) (DRAWING_FILE_SIZE - WebpSuggester.SIZE_REDUCTION_THRESHOLD_BYTES - 100);
    byte[] webpBytes = new byte[webpSize];
    fakeConverter.setFakeData(webpBytes);
    SystemFileData systemFileData =
        new SystemFileData(getTestDataFile(PNG_DRAWING), Paths.get("drawing.png"));

    List<Suggestion> suggestions =
        webpSuggester.processProjectEntry(
            GradleContext.create(MIN_SDK_VERSION_SUPPORTING_LOSSLESS_WEBP, false), systemFileData);

    assertThat(suggestions)
        .containsExactly(
            Suggestion.create(
                Suggestion.IssueType.WEBP,
                Suggestion.Category.WEBP,
                Payload.newBuilder()
                    .setWebpData(
                        WebpData.newBuilder()
                            .setFile(FileEntryData.newBuilder().setFilePath("drawing.png")))
                    .build(),
                "Convert drawing.png to webp with lossless encoding",
                200L,
                new WebpAutoFix(systemFileData.getSystemPath())));
  }

  @Test
  public void losslessReducibleDrawing_unsupportedApiLevel_noSuggestion() throws IOException {
    SystemFileData systemFileData =
        new SystemFileData(getTestDataFile(PNG_DRAWING), Paths.get("drawing.png"));
    List<Suggestion> suggestions =
        new WebpSuggester()
            .processBundleZipEntry(
                BundleContext.create(MIN_SDK_VERSION_SUPPORTING_LOSSLESS_WEBP - 1), systemFileData);

    assertThat(suggestions).isEmpty();
  }

  @Test
  public void nonLosslessReduciblePhoto_supportedApiLevel_noSuggestion() throws IOException {
    SystemFileData systemFileData =
        new SystemFileData(getTestDataFile(JPG_PHOTO), Paths.get("photo.jpg"));
    List<Suggestion> suggestions =
        new WebpSuggester()
            .processBundleZipEntry(
                BundleContext.create(MIN_SDK_VERSION_SUPPORTING_LOSSLESS_WEBP), systemFileData);

    assertThat(suggestions).isEmpty();
  }

  @Test
  public void reductionBelowThreshold_noSuggestion() throws IOException {
    FakeWebpConverter fakeConverter = new FakeWebpConverter();
    WebpSuggester webpSuggester = new WebpSuggester(fakeConverter);
    int targetOutputSizeAboveThreshold =
        (int) (DRAWING_FILE_SIZE - WebpSuggester.SIZE_REDUCTION_THRESHOLD_BYTES + 1);
    fakeConverter.setFakeData(new byte[targetOutputSizeAboveThreshold]);
    SystemFileData systemFileData =
        new SystemFileData(getTestDataFile(PNG_DRAWING), Paths.get("drawing.png"));

    List<Suggestion> suggestions =
        webpSuggester.processBundleZipEntry(
            BundleContext.create(MIN_SDK_VERSION_SUPPORTING_LOSSLESS_WEBP), systemFileData);

    assertThat(suggestions).isEmpty();
  }
}
