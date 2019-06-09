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

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.android.tools.build.bundletool.model.AndroidManifest;
import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleModule;
import com.android.tools.build.bundletool.model.BundleModuleName;
import com.android.tools.sizereduction.analyzer.model.BundleContext;
import com.android.tools.sizereduction.analyzer.model.ZipFileData;
import com.android.tools.sizereduction.analyzer.suggesters.BundleEntrySuggester;
import com.android.tools.sizereduction.analyzer.suggesters.BundleSuggester;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Enumeration;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.CheckReturnValue;

/** Analyzes an app bundle by applying the provided list of suggesters to the bundle artifact. */
public final class BundleAnalyzer implements ArtifactAnalyzer {

  private final ImmutableList<BundleSuggester> bundleSuggesters;
  private final ImmutableList<BundleEntrySuggester> bundleEntrySuggesters;

  public BundleAnalyzer(
      ImmutableList<BundleSuggester> bundleSuggesters,
      ImmutableList<BundleEntrySuggester> bundleEntrySuggesters) {
    this.bundleSuggesters = bundleSuggesters;
    this.bundleEntrySuggesters = bundleEntrySuggesters;
  }

  /** Analyzes the given bundle file for size optimization suggestions. */
  @Override
  @CheckReturnValue
  public ImmutableList<Suggestion> analyze(File artifactFile) {
    ImmutableList.Builder<Suggestion> resultBuilder = ImmutableList.<Suggestion>builder();
    try (ZipFile zipFile = new ZipFile(artifactFile)) {
      AppBundle appBundle = AppBundle.buildFromZip(zipFile);
      ImmutableMap<BundleModuleName, BundleContext> contextPerModule =
          appBundle.getModules().entrySet().stream()
              .collect(
                  toImmutableMap(
                      entry -> entry.getKey(), entry -> createContext(entry.getValue())));

      // Process suggesters operating on the entire bundle.
      BundleContext baseContext =
          contextPerModule.get(BundleModuleName.create(BundleModuleName.BASE_MODULE_NAME));
      bundleSuggesters.forEach(
          suggester ->
              resultBuilder.addAll(suggester.processBundle(baseContext, appBundle, zipFile)));

      // Process suggesters operating on the individual bundle files.
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        BundleContext context = findContext(contextPerModule, entry);
        for (BundleEntrySuggester suggester : bundleEntrySuggesters) {
          ZipFileData zipFileData = new ZipFileData(zipFile, entry);
          resultBuilder.addAll(suggester.processBundleZipEntry(context, zipFileData));
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return resultBuilder.build();
  }

  private BundleContext findContext(
      ImmutableMap<BundleModuleName, BundleContext> contextPerModule, ZipEntry entry) {
    Optional<BundleModuleName> moduleName = AppBundle.extractModuleName(entry);
    if (moduleName.isPresent() && contextPerModule.containsKey(moduleName.get())) {
      return contextPerModule.get(moduleName.get());
    }
    // Default to the base context
    return contextPerModule.get(BundleModuleName.create(BundleModuleName.BASE_MODULE_NAME));
  }

  private static BundleContext createContext(BundleModule module) {
    AndroidManifest manifest = module.getAndroidManifest();
    return BundleContext.create(
        manifest.getEffectiveMinSdkVersion(), module.getModuleMetadata().getOnDemand());
  }
}
