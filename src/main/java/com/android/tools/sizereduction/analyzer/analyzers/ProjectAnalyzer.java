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

import com.android.tools.sizereduction.analyzer.model.GradleContext;
import com.android.tools.sizereduction.analyzer.model.Project;
import com.android.tools.sizereduction.analyzer.model.SystemFileData;
import com.android.tools.sizereduction.analyzer.suggesters.ProjectSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.ProjectTreeSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.CheckReturnValue;

/**
 * Analyzes an android studio project by applying the provided list of suggesters to the project
 * directory.
 */
public final class ProjectAnalyzer implements ArtifactAnalyzer {

  private final ImmutableList<ProjectTreeSuggester> suggesters;
  private final ImmutableList<ProjectSuggester> projectSuggesters;

  public ProjectAnalyzer(
      ImmutableList<ProjectSuggester> projectSuggesters,
      ImmutableList<ProjectTreeSuggester> suggesters) {
    this.projectSuggesters = projectSuggesters;
    this.suggesters = suggesters;
  }

  /** Analyzes the given project directory for size optimization suggestions. */
  @Override
  @CheckReturnValue
  public ImmutableList<Suggestion> analyze(File projectDirectory) {
    File buildFile = new File(projectDirectory, Project.BUILD_GRADLE);
    Project project = buildFile.exists() ? Project.create(projectDirectory, null) : null;
    return analyzeProject(
        projectDirectory, project, projectDirectory, projectSuggesters, suggesters);
  }

  private static ImmutableList<Suggestion> analyzeProject(
      File rootDirectory,
      Project project,
      File directory,
      ImmutableList<ProjectSuggester> projectSuggesters,
      ImmutableList<ProjectTreeSuggester> suggesters) {
    ImmutableList.Builder<Suggestion> resultBuilder = ImmutableList.<Suggestion>builder();
    if (project != null) {
      for (ProjectSuggester projectSuggester : projectSuggesters) {
        resultBuilder.addAll(
            projectSuggester.processProject(project.getContext(), project.getProjectDirectory()));
      }
    }
    return resultBuilder
        .addAll(analyzeDirectory(rootDirectory, project, directory, projectSuggesters, suggesters))
        .build();
  }

  private static ImmutableList<Suggestion> analyzeDirectory(
      File rootDirectory,
      Project project,
      File directory,
      ImmutableList<ProjectSuggester> projectSuggesters,
      ImmutableList<ProjectTreeSuggester> suggesters) {
    ImmutableList.Builder<Suggestion> resultBuilder = ImmutableList.<Suggestion>builder();
    File[] files = directory.listFiles();
    for (File file : files) {
      String name = file.getName();
      if (name.equals(".gradle") || name.equals(".idea") || name.equals("build")) {
        continue;
      }
      if (file.isDirectory()) {
        File buildFile = new File(file, Project.BUILD_GRADLE);
        if (buildFile.exists()) {
          Project subProject = Project.create(file, project);
          resultBuilder.addAll(
              analyzeProject(rootDirectory, subProject, file, projectSuggesters, suggesters));
        } else {
          // recurse, through directory under the same directory.
          resultBuilder.addAll(
              analyzeDirectory(rootDirectory, project, file, projectSuggesters, suggesters));
        }
      } else {
        GradleContext context =
            project != null ? project.getContext() : GradleContext.create(1, false);
        Path pathWithinModule =
            project != null
                ? Paths.get(project.getProjectDirectory().getPath())
                    .relativize(Paths.get(file.getPath()))
                : Paths.get(file.getName());
        Path pathWithinRoot =
            Paths.get(rootDirectory.getPath()).relativize(Paths.get(file.getPath()));
        for (ProjectTreeSuggester suggester : suggesters) {
          SystemFileData systemFileData =
              new SystemFileData(file, pathWithinRoot, pathWithinModule);
          resultBuilder.addAll(suggester.processProjectEntry(context, systemFileData));
        }
      }
    }

    return resultBuilder.build();
  }
}
