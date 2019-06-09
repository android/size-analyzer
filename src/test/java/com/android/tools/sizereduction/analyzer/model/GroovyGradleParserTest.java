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

package com.android.tools.sizereduction.analyzer.model;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.android.tools.sizereduction.analyzer.utils.TestUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class GroovyGradleParserTest {

  private static final String PROJECT_BUILD_FILE = "projects/simple_bundle_app/build.gradle";
  private static final String APP_BUILD_FILE = "projects/simple_bundle_app/app/build.gradle";
  private static final String DYNAMIC_FEATURE_BUILD_FILE =
      "projects/simple_bundle_app/dynamic_feature/build.gradle";
  private static final String PROGUARD_BUILD_FILE = "proguard_configs.build.gradle";
  private static final String VARIABLE_MINSDK_BUILD_FILE = "variable_minSdkVersion.build.gradle";
  private static final String COMPLEX_BUILD_FILE = "complex.build.gradle";
  private static final String DISABLE_SPLITS_BUILD_FILE =
      "bundle_configs/disableSplits.build.gradle";
  private static final String MIX_BUNDLE_SPLITS_ENABLED_BUILD_FILE =
      "bundle_configs/splitEnableMix.build.gradle";

  @Test
  public void parsesPluginType_Application() throws Exception {
    File buildFile = TestUtils.getTestDataFile(APP_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1, null).build();
    assertThat(context.getPluginType()).isEqualTo(GradleContext.PluginType.APPLICATION);
  }

  @Test
  public void parsesPluginType_DynamicFeature() throws Exception {
    File buildFile = TestUtils.getTestDataFile(DYNAMIC_FEATURE_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1, null).build();
    assertThat(context.getPluginType()).isEqualTo(GradleContext.PluginType.DYNAMIC_FEATURE);
  }

  @Test
  public void parsesPluginType_Unknown() throws Exception {
    File buildFile = TestUtils.getTestDataFile(PROJECT_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1, null).build();
    assertThat(context.getPluginType()).isEqualTo(GradleContext.PluginType.UNKNOWN);
  }

  @Test
  public void parsesMinSdkVersion() throws Exception {
    File buildFile = TestUtils.getTestDataFile(APP_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1, null).build();
    assertThat(context.getMinSdkVersion()).isEqualTo(15);
  }

  @Test
  public void parsesMinSdkVersionInTupleExpression() throws Exception {
    File buildFile = TestUtils.getTestDataFile(DYNAMIC_FEATURE_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1, null).build();
    assertThat(context.getMinSdkVersion()).isEqualTo(14);
  }

  @Test
  public void parsesProguardConfigs() throws Exception {
    File buildFile = TestUtils.getTestDataFile(PROGUARD_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1, null).build();
    ImmutableMap<String, ProguardConfig> expectedMap =
        ImmutableMap.of(
            "release",
            ProguardConfig.builder()
                .setMinifyEnabled(true)
                .setHasProguardRules(true)
                .setObfuscationEnabled(true)
                .build(),
            "shrinkOnly",
            ProguardConfig.builder()
                .setMinifyEnabled(true)
                .setHasProguardRules(true)
                .setObfuscationEnabled(false)
                .build(),
            "debug",
            ProguardConfig.builder()
                .setMinifyEnabled(false)
                .setHasProguardRules(true)
                .setObfuscationEnabled(true)
                .build());

    assertThat(context.getProguardConfigs()).isEqualTo(expectedMap);
  }

  @Test
  public void parsesComplexBuildFile() throws Exception {
    File buildFile = TestUtils.getTestDataFile(COMPLEX_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1, null).build();
    ImmutableMap<String, ProguardConfig> expectedMap =
        ImmutableMap.of(
            "release",
            ProguardConfig.builder()
                .setMinifyEnabled(true)
                .setHasProguardRules(true)
                .setObfuscationEnabled(true)
                .build(),
            "debug",
            ProguardConfig.builder()
                .setMinifyEnabled(true)
                .setHasProguardRules(true)
                .setObfuscationEnabled(true)
                .build());

    assertThat(context.getMinSdkVersion()).isEqualTo(19);
    assertThat(context.getPluginType()).isEqualTo(GradleContext.PluginType.APPLICATION);
    assertThat(context.getProguardConfigs()).isEqualTo(expectedMap);
  }

  @Test
  public void variableMinSdkVersionDefaultsToDefaultvalue() throws Exception {
    File buildFile = TestUtils.getTestDataFile(VARIABLE_MINSDK_BUILD_FILE);
    int defaultMinSdkVersion = 345;
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context =
        GroovyGradleParser.parseGradleBuildFile(content, defaultMinSdkVersion, null).build();
    assertThat(context.getMinSdkVersion()).isEqualTo(defaultMinSdkVersion);
  }

  @Test
  public void setsAndroidPluginVersion() throws Exception {
    File buildFile = TestUtils.getTestDataFile(PROJECT_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1, null).build();

    assertThat(context.getAndroidPluginVersion()).isNotNull();
    assertThat(context.getAndroidPluginVersion().getMajorVersion()).isEqualTo(3);
    assertThat(context.getAndroidPluginVersion().getMinorVersion()).isEqualTo(4);
  }

  @Test
  public void setsBundleConfigAllFalse() throws Exception {
    File buildFile = TestUtils.getTestDataFile(DISABLE_SPLITS_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1, null).build();

    assertThat(context.getBundleConfig().getAbiSplitEnabled()).isFalse();
    assertThat(context.getBundleConfig().getDensitySplitEnabled()).isFalse();
    assertThat(context.getBundleConfig().getLanguageSplitEnabled()).isFalse();
  }

  @Test
  public void setsBundleConfigEnabledByDefault() throws Exception {
    File buildFile = TestUtils.getTestDataFile(APP_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1, null).build();

    assertThat(context.getBundleConfig().getAbiSplitEnabled()).isTrue();
    assertThat(context.getBundleConfig().getDensitySplitEnabled()).isTrue();
    assertThat(context.getBundleConfig().getLanguageSplitEnabled()).isTrue();
  }

  @Test
  public void setsBundleConfigMixEnabled() throws Exception {
    File buildFile = TestUtils.getTestDataFile(MIX_BUNDLE_SPLITS_ENABLED_BUILD_FILE);
    String content = Files.asCharSource(buildFile, UTF_8).read();
    GradleContext context = GroovyGradleParser.parseGradleBuildFile(content, 1, null).build();

    assertThat(context.getBundleConfig().getAbiSplitEnabled()).isTrue();
    assertThat(context.getBundleConfig().getDensitySplitEnabled()).isTrue();
    assertThat(context.getBundleConfig().getLanguageSplitEnabled()).isFalse();
  }
}
