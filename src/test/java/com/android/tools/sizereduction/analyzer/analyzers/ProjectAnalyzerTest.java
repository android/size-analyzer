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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.truth.Truth.assertThat;

import com.android.tools.sizereduction.analyzer.SuggestionPayload.Payload;
import com.android.tools.sizereduction.analyzer.model.AndroidPluginVersion;
import com.android.tools.sizereduction.analyzer.model.GradleContext;
import com.android.tools.sizereduction.analyzer.model.ProguardConfig;
import com.android.tools.sizereduction.analyzer.suggesters.ProjectSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.ProjectTreeSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.testing.FakeSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.testing.FakeSuggester.ContextAndEntryPath;
import com.android.tools.sizereduction.analyzer.utils.TestUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class ProjectAnalyzerTest {

  private static final String ROOT_PROJECT = "projects/simple_bundle_app";
  private static final String APP_PROJECT = "projects/simple_bundle_app/app";
  private static final String IGNORABLE_FILES_PROJECT = "projects/app_with_ignorable_files";

  private final FakeSuggester suggester = new FakeSuggester();
  private ImmutableList<ProjectTreeSuggester> testSuggesters;
  private ImmutableList<ProjectSuggester> testProjectSuggesters;

  @Before
  public void setUp() {
    testProjectSuggesters = ImmutableList.of(suggester);
    testSuggesters = ImmutableList.of(suggester);
  }

  @Test
  public void analyze_iteratesOverAppProject() throws Exception {
    ProjectAnalyzer analyzer = new ProjectAnalyzer(testProjectSuggesters, testSuggesters);
    File appProject = TestUtils.getTestDataFile(APP_PROJECT);
    GradleContext context =
        GradleContext.builder()
            .setMinSdkVersion(15)
            .setOnDemand(false)
            .setPluginType(GradleContext.PluginType.APPLICATION)
            .setProguardConfigs(
                ImmutableMap.of(
                    "release",
                    ProguardConfig.builder()
                        .setMinifyEnabled(false)
                        .setHasProguardRules(true)
                        .build()))
            .build();
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
            Suggestion.IssueType.PROGUARD_NO_OBFUSCATION,
            Suggestion.Category.PROGUARD,
            Payload.getDefaultInstance(),
            "Stub Artifact Suggestion",
            /* estimatedBytesSaved= */ null,
            /* autoFix= */ null);
    suggester.setArtifactSuggestions(ImmutableList.of(stubArtifactSuggestion));
    suggester.setEntrySuggestions(
        ImmutableMultimap.of(
            ContextAndEntryPath.create(context, "src/main/AndroidManifest.xml"), stubSuggestion));

    ImmutableList<Suggestion> suggestions = analyzer.analyze(appProject);

    assertThat(suggestions).containsExactly(stubSuggestion, stubArtifactSuggestion);
    assertThat(suggester.getAnalyzedEntries())
        .containsExactlyElementsIn(filesUnderDirectory(appProject.toPath()));
  }

  @Test
  public void analyze_iteratesOverRootProject() throws Exception {
    ProjectAnalyzer analyzer = new ProjectAnalyzer(testProjectSuggesters, testSuggesters);
    File rootProject = TestUtils.getTestDataFile(ROOT_PROJECT);
    GradleContext appContext =
        GradleContext.builder()
            .setMinSdkVersion(15)
            .setOnDemand(false)
            .setPluginType(GradleContext.PluginType.APPLICATION)
            .setProguardConfigs(
                ImmutableMap.of(
                    "release",
                    ProguardConfig.builder()
                        .setMinifyEnabled(false)
                        .setHasProguardRules(true)
                        .build()))
            .setAndroidPluginVersion(AndroidPluginVersion.create("3.4.0"))
            .build();
    Suggestion stubSuggestion =
        Suggestion.create(
            Suggestion.IssueType.WEBP,
            Suggestion.Category.WEBP,
            Payload.getDefaultInstance(),
            "Stub Suggestion",
            /* estimatedBytesSaved= */ null,
            /* autoFix= */ null);
    suggester.setEntrySuggestions(
        ImmutableMultimap.of(
            ContextAndEntryPath.create(appContext, "app/src/main/AndroidManifest.xml"),
            stubSuggestion));

    ImmutableList<Suggestion> suggestions = analyzer.analyze(rootProject);

    assertThat(suggestions).containsExactly(stubSuggestion);
    assertThat(suggester.getAnalyzedEntries())
        .containsExactlyElementsIn(filesUnderDirectory(rootProject.toPath()));
  }

  @Test
  public void analyze_ignoreNonProjectFiles() throws Exception {
    ProjectAnalyzer analyzer = new ProjectAnalyzer(testProjectSuggesters, testSuggesters);
    File appProject = TestUtils.getTestDataFile(IGNORABLE_FILES_PROJECT);

    ImmutableList<Suggestion> suggestions = analyzer.analyze(appProject);

    assertThat(suggestions).isEmpty();
    assertThat(suggester.getAnalyzedEntries()).containsExactly("build.gradle");
  }

  private static ImmutableSet<String> filesUnderDirectory(Path directory) throws Exception {
    try (Stream<Path> fileStream = Files.walk(directory)) {
      return fileStream
          .filter(Files::isRegularFile)
          .map(filePath -> directory.toUri().relativize(filePath.toUri()).toString())
          .collect(toImmutableSet());
    }
  }
}
