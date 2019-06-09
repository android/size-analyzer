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

package com.android.tools.sizereduction.analyzer.cli;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparingLong;

import com.android.tools.sizereduction.analyzer.suggesters.AutoFix;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion;
import com.android.tools.sizereduction.analyzer.suggesters.Suggestion.Category;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

/**
 * This class will print out the suggestions in an understandable format in the terminal.
 * Suggestions will be categorized and sorted by size reduction savings estimate.
 */
public final class TerminalInterface {

  private static final ImmutableMap<Category, String> CATEGORY_TO_STRING =
      ImmutableMap.of(
          Category.WEBP, "Converting images to webp saves up to ",
          Category.LARGE_FILES, "Dynamically delivering large files saves up to ",
          Category.PROGUARD, "Efficiently configuring proguard can save up to ");
  private static final ImmutableMap<String, Category> STRING_TO_CATEGORY =
      ImmutableMap.of(
          "webp",
          Category.WEBP,
          "large-files",
          Category.LARGE_FILES,
          "proguard",
          Category.PROGUARD);

  private final ImmutableList<Suggestion> suggestions;
  private final ImmutableList<Suggestion.Category> displayCategories;
  private final boolean displayDetails;
  private final boolean applyFixes;
  private final boolean showFixes;
  private final Scanner inputScanner;

  public static TerminalInterface create(
      ImmutableList<Suggestion> suggestions,
      ImmutableList<String> categories,
      boolean displayDetails) {
    return TerminalInterface.create(
        suggestions, categories, displayDetails, /* applyFixes= */ false, /* showFixes= */ false);
  }

  public static TerminalInterface create(
      ImmutableList<Suggestion> suggestions,
      ImmutableList<String> categories,
      boolean displayDetails,
      boolean applyFixes,
      boolean showFixes) {
    return TerminalInterface.create(
        suggestions,
        categories,
        displayDetails,
        applyFixes,
        showFixes,
        new Scanner(System.in, UTF_8.name()));
  }

  @VisibleForTesting
  static TerminalInterface create(
      ImmutableList<Suggestion> suggestions,
      ImmutableList<String> categories,
      boolean displayDetails,
      boolean applyFixes,
      boolean showFixes,
      Scanner inputScanner) {
    return new TerminalInterface(
        suggestions,
        categories.stream()
            .filter(s -> STRING_TO_CATEGORY.containsKey(s))
            .map(s -> STRING_TO_CATEGORY.get(s))
            .collect(toImmutableList()),
        displayDetails,
        applyFixes,
        showFixes,
        inputScanner);
  }

  private TerminalInterface(
      ImmutableList<Suggestion> suggestions,
      ImmutableList<Suggestion.Category> displayCategories,
      boolean displayDetails,
      boolean applyFixes,
      boolean showFixes,
      Scanner inputScanner) {
    this.suggestions = suggestions;
    this.displayCategories = displayCategories;
    this.displayDetails = displayDetails;
    this.applyFixes = applyFixes;
    this.showFixes = showFixes;
    this.inputScanner = inputScanner;
  }

  public void displaySuggestions() {
    ImmutableListMultimap<Category, Suggestion> categorizedSuggestions = categorizeSuggestions();
    if (categorizedSuggestions.size() == 0) {
      System.out.println("No size saving suggestions found.");
      return;
    }
    ImmutableList<Category> categoryDisplayOrder = getCategoryDisplayOrder(categorizedSuggestions);

    Long runningTotal = 0L;
    for (Category category : categoryDisplayOrder) {
      ImmutableList<Suggestion> suggestions = categorizedSuggestions.get(category);
      Long totalSavings = getBytesSavedForSuggestionList(suggestions);
      System.out.println(
          Ansi.ansi()
              .fg(Color.GREEN)
              .a(CATEGORY_TO_STRING.get(category))
              .fg(Color.RED)
              .a(humanReadableByteCount(totalSavings))
              .reset());
      if (showFixes) {
        applyFixesInteractively(suggestions, category);
      } else {
        if (displayDetails) {
          suggestions.forEach(TerminalInterface::prettyPrintSuggestion);
        }
        if (applyFixes) {
          System.out.println("applying all available fixes for category: " + category);
          suggestions.stream()
              .filter(suggestion -> suggestion.getAutoFix() != null)
              .map(Suggestion::getAutoFix)
              .forEach(AutoFix::apply);
        }
      }
      runningTotal += totalSavings;
    }

    // Print out total size savings suggested.
    System.out.println(
        Ansi.ansi()
            .fg(Color.GREEN)
            .a("Total size savings of ")
            .fg(Color.RED)
            .a(humanReadableByteCount(runningTotal))
            .reset()
            .a(" found.")
            .reset());
    // if we are only showing suggestions, let them know of other available options.
    if (!applyFixes && !showFixes) {
      if (!displayDetails) {
        System.out.println(
            "The -d flag will display a list of individual suggestions for each category.");
      }
      // if there are any suggestions with a fix, explicitly let developers know they can apply them
      // or be shown them.
      if (this.suggestions.stream().anyMatch(suggestion -> suggestion.getAutoFix() != null)) {
        System.out.println(
            "The --apply-all flag will automatically apply any available fixes while"
                + " the --show-fixes flag allows for fixes to be selectively applied.");
      }
    }
  }

  @VisibleForTesting
  void applyFixesInteractively(ImmutableList<Suggestion> suggestions, Category category) {
    int currentFixNumber = 0;
    Map<Integer, AutoFix> autoFixMap = new HashMap<>();
    for (Suggestion suggestion : suggestions) {
      AutoFix autoFix = suggestion.getAutoFix();
      if (autoFix == null) {
        continue;
      }
      if (currentFixNumber == 0) {
        System.out.println("0) Apply-all");
        ++currentFixNumber;
      }
      autoFixMap.put(currentFixNumber, autoFix);
      System.out.println(
          currentFixNumber + ") " + suggestion.getMessage() + getBytesSavedString(suggestion));
      ++currentFixNumber;
    }
    if (autoFixMap.isEmpty()) {
      System.out.println("No fixes currently available for category: " + category);
      return;
    }
    System.out.println(
        "Select the fixes you would like to apply by typing in the numbers separated by commas or"
            + " spaces. Type 0 to apply all fixes.");
    if (!inputScanner.hasNextLine()) {
      return;
    }
    String input = inputScanner.nextLine();
    Iterable<String> entries =
        Splitter.on(CharMatcher.anyOf(", ")).omitEmptyStrings().trimResults().split(input);
    if (Streams.stream(entries).anyMatch(entry -> entry.equals("0"))) {
      autoFixMap.values().forEach(AutoFix::apply);
    } else {
      for (String entry : entries) {
        try {
          Integer fixNumber = Integer.parseInt(entry);
          if (fixNumber >= 0 && fixNumber < autoFixMap.size()) {
            autoFixMap.get(fixNumber).apply();
          } else {
            System.out.println("Fix #" + entry + " was not in the list; skipping it.");
          }
        } catch (NumberFormatException e) {
          System.out.println("Fix #" + entry + " was not in the list; skipping it.");
        }
      }
    }
    System.out.println("Applied selected fixes.");
  }

  /**
   * Sorts each suggestion into its corresponding category and sorts them by size savings within
   * each category.
   */
  @VisibleForTesting
  ImmutableListMultimap<Category, Suggestion> categorizeSuggestions() {
    return suggestions.stream()
        .filter(
            suggestion ->
                displayCategories.isEmpty() || displayCategories.contains(suggestion.getCategory()))
        .sorted(comparingLong(TerminalInterface::getBytesSavedForSuggestion).reversed())
        .collect(
            toImmutableListMultimap(
                suggestion -> suggestion.getCategory(), suggestion -> suggestion));
  }

  /**
   * Returns the order for displaying each category, sorted by total size savings for each category.
   */
  @VisibleForTesting
  ImmutableList<Category> getCategoryDisplayOrder(
      ImmutableListMultimap<Category, Suggestion> categorizedSuggestions) {
    return categorizedSuggestions.asMap().entrySet().stream()
        .sorted(comparingLong(TerminalInterface::getTotalSizeSavings).reversed())
        .map(entry -> entry.getKey())
        .collect(toImmutableList());
  }

  private static void prettyPrintSuggestion(Suggestion suggestion) {
    System.out.println(
        Ansi.ansi()
            .a(suggestion.getMessage())
            .fg(Color.RED)
            .a(getBytesSavedString(suggestion))
            .reset());
  }

  private static String getBytesSavedString(Suggestion suggestion) {
    return " (saves " + humanReadableByteCount(suggestion.getEstimatedBytesSaved()) + ")";
  }

  private static Long getTotalSizeSavings(
      Map.Entry<Category, Collection<Suggestion>> mappedSuggestions) {
    return getBytesSavedForSuggestionList(mappedSuggestions.getValue());
  }

  private static Long getBytesSavedForSuggestion(Suggestion suggestion) {
    return suggestion.getEstimatedBytesSaved() != null ? suggestion.getEstimatedBytesSaved() : 0L;
  }

  private static Long getBytesSavedForSuggestionList(Collection<Suggestion> suggestions) {
    return suggestions.stream()
        .mapToLong(suggestion -> getBytesSavedForSuggestion(suggestion))
        .sum();
  }

  private static String humanReadableByteCount(Long bytes) {
    if (bytes == null || bytes == 0) {
      return "unknown bytes";
    }
    final int unit = 1024;
    if (bytes < unit) {
      return bytes + " B";
    }
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    String pre = "kMGTPE".charAt(exp - 1) + "i";
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }
}
