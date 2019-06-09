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
 * limitations under the License.
 */

package com.android.tools.sizereduction.analyzer.model;

import java.util.Locale;

/** Holds static information of the current operating system. */
public final class SystemInformation {

  private SystemInformation() {}

  private static final String ARCH_DATA_MODEL = System.getProperty("sun.arch.data.model");
  private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.US);

  public static boolean is32Bit() {
    return ARCH_DATA_MODEL == null || ARCH_DATA_MODEL.equals("32");
  }

  public static boolean isWindows() {
    return OS_NAME.startsWith("windows");
  }

  public static boolean isMac() {
    return OS_NAME.startsWith("mac");
  }
}
