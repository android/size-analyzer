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

import com.android.tools.sizereduction.analyzer.SuggestionPayload.FileEntryData;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload.WebpData;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.model.Context;
import com.android.tools.sizereduction.analyzer.model.FileData;
import com.android.tools.sizereduction.analyzer.model.GradleContext;
import com.android.tools.sizereduction.analyzer.model.SystemFileData;
import com.android.tools.sizereduction.analyzer.suggesters.BundleEntrySuggester;
import com.android.tools.sizereduction.analyzer.suggesters.ProjectTreeSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CountingInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

/** Suggests images that could be reduced in size by converting to webp. */
public class WebpSuggester implements BundleEntrySuggester, ProjectTreeSuggester {

  // Source: https://developer.android.com/studio/write/convert-webp
  @VisibleForTesting static final int MIN_SDK_VERSION_SUPPORTING_LOSSLESS_WEBP = 18;

  private static final ImmutableSet<String> SUPPORTED_FILE_TYPES =
      ImmutableSet.of("bmp", "png", "jpg", "jpeg");
  private static final long ESTIMATE_PRECISION = 100;
  @VisibleForTesting static final long SIZE_REDUCTION_THRESHOLD_BYTES = ESTIMATE_PRECISION;

  private final WebpConverter webpConverter;

  public WebpSuggester() {
    this(new WebpConverterImpl());
  }

  @VisibleForTesting
  WebpSuggester(WebpConverter webpConverter) {
    this.webpConverter = webpConverter;
  }

  @Override
  public ImmutableList<Suggestion> processBundleZipEntry(BundleContext context, FileData fileData) {
    return processFileEntry(context, fileData);
  }

  @Override
  public ImmutableList<Suggestion> processProjectEntry(GradleContext context, FileData fileData) {
    return processFileEntry(context, fileData);
  }

  private ImmutableList<Suggestion> processFileEntry(Context context, FileData fileData) {
    if (context.getMinSdkVersion() < MIN_SDK_VERSION_SUPPORTING_LOSSLESS_WEBP) {
      return ImmutableList.of();
    }

    if (!SUPPORTED_FILE_TYPES.contains(getFileExtension(fileData))) {
      return ImmutableList.of();
    }

    try (InputStream inputStream = fileData.getInputStream()) {
      CountingInputStream countingStream = new CountingInputStream(inputStream);
      BufferedImage bufferedImage = safelyParseImage(countingStream);

      long oldSize = countingStream.getCount();
      byte[] webpBytes = webpConverter.encodeLosslessWebp(bufferedImage);
      long newSize = webpBytes.length;
      long reduction = oldSize - newSize;

      if (reduction >= SIZE_REDUCTION_THRESHOLD_BYTES) {
        // We must round off the estimate to account for slight differences between different
        // versions of the webp tools (cwebp uses a higher effort factor by default than libwebp,
        // and we have no way of controlling it given this API).
        // We don't want to seem to promise a specific size reduction so we round down to the
        // nearest
        // round number.
        long estimate = reduction - (reduction % ESTIMATE_PRECISION);
        WebpAutoFix autoFix = null;
        if (fileData instanceof SystemFileData) {
          autoFix = new WebpAutoFix(((SystemFileData) fileData).getSystemPath());
        }

        return ImmutableList.of(
            Suggestion.create(
                Suggestion.IssueType.WEBP,
                Suggestion.Category.WEBP,
                Payload.newBuilder()
                    .setWebpData(
                        WebpData.newBuilder()
                            .setFile(
                                FileEntryData.newBuilder()
                                    .setFilePath(fileData.getPathWithinRoot().toString())))
                    .build(),
                "Convert " + fileData.getPathWithinRoot() + " to webp with lossless encoding",
                estimate,
                autoFix));
      } else {
        return ImmutableList.of();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static BufferedImage safelyParseImage(InputStream inputStream) {
    try {
      return Imaging.getBufferedImage(inputStream);
    } catch (IOException | ImageReadException e) {
      throw new RuntimeException(e);
    }
  }
}
