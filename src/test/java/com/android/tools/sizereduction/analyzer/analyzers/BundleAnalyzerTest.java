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

package com.android.tools.sizereduction.analyzer.analyzers;

import static com.google.common.truth.Truth.assertThat;

import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.suggesters.BundleEntrySuggester;
import com.android.tools.sizereduction.analyzer.suggesters.BundleSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.testing.FakeSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.testing.FakeSuggester.ContextAndEntryPath;
import com.android.tools.sizereduction.analyzer.utils.TestUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import java.io.File;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class BundleAnalyzerTest {

  private static final String APP_BUNDLE = "app.aab";

  private final FakeSuggester suggester = new FakeSuggester();
  private ImmutableList<BundleSuggester> testArtifactSuggesters;
  private ImmutableList<BundleEntrySuggester> testEntrySuggesters;

  @Before
  public void setUp() {
    testArtifactSuggesters = ImmutableList.of(suggester);
    testEntrySuggesters = ImmutableList.of(suggester);
  }

  @Test
  public void analyze_callsSuggesters() throws Exception {
    BundleAnalyzer analyzer = new BundleAnalyzer(testArtifactSuggesters, testEntrySuggesters);
    File bundleFile = TestUtils.getTestDataFile(APP_BUNDLE);
    BundleContext context = BundleContext.create(/* minSdkVersion= */ 23);
    Suggestion stubSuggestion =
        Suggestion.create(
            Suggestion.IssueType.WEBP,
            Suggestion.Category.WEBP,
            Payload.getDefaultInstance(),
            "Stub Suggestion",
            /* estimatedBytesSaved= */ null,
            /* autoFix= */ null);
    Suggestion stubArtifactSuggestion =
        Suggestion.create(
            Suggestion.IssueType.WEBP,
            Suggestion.Category.WEBP,
            Payload.getDefaultInstance(),
            "Stub Artifact Suggestion",
            /* estimatedBytesSaved= */ null,
            /* autoFix= */ null);
    suggester.setEntrySuggestions(
        ImmutableMultimap.of(
            ContextAndEntryPath.create(context, "base/manifest/AndroidManifest.xml"),
            stubSuggestion));
    suggester.setArtifactSuggestions(ImmutableList.of(stubArtifactSuggestion));

    ImmutableList<Suggestion> suggestions = analyzer.analyze(bundleFile);

    assertThat(suggestions).containsExactly(stubSuggestion, stubArtifactSuggestion);
  }
}
