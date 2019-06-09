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

/**
 * Context interface for the artifact being analyzed, which will be used by the suggester when it is
 * called.
 */
public interface Context {

  /** The min sdk version declared for this artifact. */
  public int getMinSdkVersion();

  /** The on demand value declared for this artifact. */
  public boolean getOnDemand();
}
