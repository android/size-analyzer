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

package com.android.tools.sizereduction.analyzer;

import com.android.tools.sizereduction.analyzer.AnalyzerMain.VersionProvider;
import com.android.tools.sizereduction.analyzer.cli.CheckBundle;
import com.android.tools.sizereduction.analyzer.cli.CheckProject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;

/** Main entry point of the size reduction analyzer tool. */
@Command(
    name = "analyzer",
    description = "Entry point for the size reduction analyzer",
    versionProvider = VersionProvider.class,
    subcommands = {
      CheckBundle.class,
      CheckProject.class,
      HelpCommand.class,
    })
public class AnalyzerMain implements Callable<Void> {
  private static final String CURRENT_VERSION = "0.2.0";

  @Option(
      names = {"-h", "--help"},
      usageHelp = true,
      description = "Shows this help message and exits")
  @SuppressWarnings("unused") // Used by picocli
  private boolean helpRequested;

  @Option(
      names = {"-v", "--version"},
      versionHelp = true,
      description = "Prints version information and exits")
  @SuppressWarnings("unused") // Used by picocli
  private boolean versionRequested;

  public static void main(String[] args) throws IOException {
    AnsiConsole.systemInstall();
    AnalyzerMain main = new AnalyzerMain();
    main.handleCommand(args);
  }

  void handleCommand(String[] args) {
    CommandLine cmd = new CommandLine(this);
    List<Object> results = cmd.parseWithHandler(new CommandLine.RunAll(), args);
    int numCommandsExecuted = results == null ? 0 : results.size();
    if (numCommandsExecuted == 1) {
      // Main CLI command run without arguments.
      cmd.usage(System.out);
    }
  }

  @Override
  public Void call() {
    // do nothing as the subcommands handle all the logic.
    return null;
  }

  /** Required to provide version info to picocli. */
  static class VersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() {
      return new String[] {CURRENT_VERSION};
    }
  }
}
