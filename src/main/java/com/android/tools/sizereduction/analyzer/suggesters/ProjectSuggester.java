/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.tools.sizereduction.analyzer.suggesters;

import com.android.tools.sizereduction.analyzer.model.GradleContext;
import com.google.common.collect.ImmutableList;
import java.io.File;

/** Interface for generating suggestions for an entire project. */
public interface ProjectSuggester {

  /** Generates suggestions for an Android Studio project directory. */
  ImmutableList<Suggestion> processProject(GradleContext context, File projectDir);
}
