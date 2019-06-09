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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.android.tools.sizereduction.analyzer.utils.TestUtils;
import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class ProjectTest {

  private static final String SIMPLE_PROJECT_DIR = "projects/simple_bundle_app";
  private static final String BASE_PROJECT_DIR = "projects/simple_bundle_app/app";
  private static final String FEATURE_PROJECT_DIR = "projects/simple_bundle_app/dynamic_feature";
  private static final String BAD_PROJECT_DIR = "projects/bad_project";

  @Test
  public void create_rootProjectSuccessfully() throws Exception {
    File simpleBundleDir = TestUtils.getTestDataFile(SIMPLE_PROJECT_DIR);
    Project project = Project.create(simpleBundleDir, null);

    assertThat(project.getContext().getMinSdkVersion()).isEqualTo(1);
    assertThat(project.getContext().getOnDemand()).isFalse();
  }

  @Test
  public void create_appProjectSuccessfully() throws Exception {
    File appDir = TestUtils.getTestDataFile(BASE_PROJECT_DIR);
    Project project = Project.create(appDir, null);

    assertThat(project.getContext().getMinSdkVersion()).isEqualTo(15);
    assertThat(project.getContext().getOnDemand()).isFalse();
  }

  @Test
  public void create_dynamicFeatureProjectSuccessfully() throws Exception {
    File featureDir = TestUtils.getTestDataFile(FEATURE_PROJECT_DIR);
    Project project = Project.create(featureDir, null);

    assertThat(project.getContext().getMinSdkVersion()).isEqualTo(14);
    assertThat(project.getContext().getOnDemand()).isTrue();
  }

  @Test
  public void create_ProjectWithParentProjectOverride() throws Exception {
    File simpleBundleDir = TestUtils.getTestDataFile(SIMPLE_PROJECT_DIR);
    Project parentProject =
        Project.builder()
            .setProjectDirectory(simpleBundleDir)
            .setContext(GradleContext.create(10))
            .build();
    Project project = Project.create(simpleBundleDir, parentProject);

    assertThat(project.getContext().getMinSdkVersion()).isEqualTo(10);
    assertThat(project.getContext().getOnDemand()).isFalse();
  }

  @Test
  public void create_ProjectWithParentProjectIgnore() throws Exception {
    File featureDir = TestUtils.getTestDataFile(FEATURE_PROJECT_DIR);
    Project parentProject =
        Project.builder()
            .setProjectDirectory(featureDir)
            .setContext(GradleContext.create(10))
            .build();
    Project project = Project.create(featureDir, parentProject);

    assertThat(project.getContext().getMinSdkVersion()).isEqualTo(14);
    assertThat(project.getContext().getOnDemand()).isTrue();
  }

  @Test
  public void create_badProjectDirFails() throws Exception {
    File badProjectDir = TestUtils.getTestDataFile(BAD_PROJECT_DIR);

    assertThrows(RuntimeException.class, () -> Project.create(badProjectDir, null));
  }
}
