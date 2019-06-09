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
import java.nio.file.Paths;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class QuestionableFilesSuggesterTest {
  // files for bundles
  private static final String ASSETS_FILE = "assets/hello.bin";
  private static final String RESOURCES_FILE = "res/raw/hello.bin";
  private static final String METADATA_FILE =
      "BUNDLE-METADATA/com/android/tools/build/libraries/dependencies.pb";
  private static final String CONFIG_FILE = "BundleConfig.pb";
  private static final String MANIFEST_FILE = "manifest/AndroidManifest.xml";
  private static final String DEX_FILE = "dex/classes.dex";
  private static final String INVALID_DEX_FILE = "dex/classes.xml";
  private static final String LIB_FILE = "lib/goog.so";
  private static final String INVALID_LIB_FILE = "lib/goog.dex";
  private static final String ROOT_MISC_FILE = "root/foobar.txt";
  private static final String META_INF_FILE = "root/META-INF/MANIFEST.MF";

  // files for android studio projects
  private static final String PROJECT_ASSET_FILE = "src/main/assets/foo.bin";
  private static final String PROJECT_RES_FILE = "src/main/res/raw/foo.bin";
  private static final String JAVA_FILE = "src/main/java/Main.java";
  private static final String PROJECT_MANIFEST_FILE = "src/main/AndroidManifest.xml";
  private static final String PROJECT_MISC_FILE = "src/main/resources/foobar.txt";
  private static final String PROJECT_ROOT_MISC_FILE = "readme.md";
  private static final String OTHER_MISC_FILE = "app/app.iml";
  private static final String GRADLE_WRAPPER = "gradle/wrapper/gradle-wrapper.jar";
  private static final String BUILD_FILE = "build/outputs/foo.apk";

  private static final long LARGE_FILE_SIZE = 1025L;
  private static final long SMALL_FILE_SIZE = 1023L;

  @Test
  public void validMetadataFile() {
    testValidFile(METADATA_FILE);
  }

  @Test
  public void validAssetsFile() {
    testValidFile(ASSETS_FILE);
  }

  @Test
  public void validConfigFile() {
    testValidFile(CONFIG_FILE);
  }

  @Test
  public void validResFile() {
    testValidFile(RESOURCES_FILE);
  }

  @Test
  public void validManifestFile() {
    testValidFile(MANIFEST_FILE);
  }

  @Test
  public void validDexFile() {
    testValidFile(DEX_FILE);
  }

  @Test
  public void invalidDexFile() {
    testInvalidFile(INVALID_DEX_FILE);
  }

  @Test
  public void validLibFile() {
    testValidFile(LIB_FILE);
  }

  @Test
  public void invalidLibFile() {
    testInvalidFile(INVALID_LIB_FILE);
  }

  @Test
  public void invalidRootFile() {
    testInvalidFile(ROOT_MISC_FILE);
  }

  @Test
  public void validRootFile() {
    testValidFile(META_INF_FILE);
  }

  @Test
  public void validProjectAssetFile() {
    testValidProjectFile(PROJECT_ASSET_FILE);
  }

  @Test
  public void validProjectResFile() {
    testValidProjectFile(PROJECT_RES_FILE);
  }

  @Test
  public void validJavaFile() {
    testValidProjectFile(JAVA_FILE);
  }

  @Test
  public void validProjectManifestFile() {
    testValidProjectFile(PROJECT_MANIFEST_FILE);
  }

  @Test
  public void invalidProjectFile() {
    testInvalidProjectFile(PROJECT_MISC_FILE);
  }

  @Test
  public void validProjectRootFile() {
    testValidProjectFile(PROJECT_ROOT_MISC_FILE);
  }

  @Test
  public void validDirectoryFile() {
    testValidProjectFile(OTHER_MISC_FILE);
  }

  @Test
  public void validGradleWrapper() {
    testValidProjectFile(GRADLE_WRAPPER);
  }

  @Test
  public void validBuildFile() {
    testValidProjectFile(BUILD_FILE);
  }

  @Test
  public void invalidSmallFile() {
    FileData fileData = FakeFileData.builder(ROOT_MISC_FILE).setSize(SMALL_FILE_SIZE).build();
    assertThat(
            new QuestionableFilesSuggester()
                .processBundleZipEntry(BundleContext.create(1, false), fileData))
        .isEmpty();
  }

  private void testValidFile(String filename) {
    FileData fileData = FakeFileData.builder(filename).setSize(LARGE_FILE_SIZE).build();
    assertThat(
            new QuestionableFilesSuggester()
                .processBundleZipEntry(BundleContext.create(1, false), fileData))
        .isEmpty();
  }

  private void testValidProjectFile(String filename) {
    FileData fileData = FakeFileData.builder(filename).setSize(LARGE_FILE_SIZE).build();
    assertThat(
            new QuestionableFilesSuggester()
                .processProjectEntry(GradleContext.create(1, false), fileData))
        .isEmpty();
  }

  private void testInvalidFile(String filename) {
    FileData fileData = FakeFileData.builder(filename).setSize(LARGE_FILE_SIZE).build();
    List<Suggestion> suggestions =
        new QuestionableFilesSuggester()
            .processBundleZipEntry(BundleContext.create(1, false), fileData);
    assertThat(suggestions)
        .containsExactly(
            Suggestion.create(
                Suggestion.IssueType.QUESTIONABLE_FILE,
                Suggestion.Category.LARGE_FILES,
                Payload.getDefaultInstance(),
                QuestionableFilesSuggester.getSuggestionMessage(Paths.get(filename)),
                LARGE_FILE_SIZE,
                /* autoFix= */ null));
  }

  private void testInvalidProjectFile(String filename) {
    FileData fileData = FakeFileData.builder(filename).setSize(LARGE_FILE_SIZE).build();
    List<Suggestion> suggestions =
        new QuestionableFilesSuggester()
            .processProjectEntry(GradleContext.create(1, false), fileData);
    assertThat(suggestions)
        .containsExactly(
            Suggestion.create(
                Suggestion.IssueType.QUESTIONABLE_FILE,
                Suggestion.Category.LARGE_FILES,
                Payload.getDefaultInstance(),
                QuestionableFilesSuggester.getSuggestionMessage(Paths.get(filename)),
                LARGE_FILE_SIZE,
                /* autoFix= */ null));
  }
}
