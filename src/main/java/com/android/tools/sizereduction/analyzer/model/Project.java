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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/** Represents an Android Studio Project. */
@AutoValue
public abstract class Project {

  public static final String BUILD_GRADLE = "build.gradle";
  private static final String MANIFEST = "src/main/AndroidManifest.xml";
  private static final String DIST_URI = "http://schemas.android.com/apk/distribution";
  private static final String VALUE_TRUE = "true";
  private static final String VALUE_1 = "1";

  public static Project create(File directory, @Nullable Project parent) {
    GradleContext context = createContext(directory, parent);
    return new AutoValue_Project.Builder()
        .setProjectDirectory(directory)
        .setContext(context)
        .build();
  }

  @VisibleForTesting
  static Builder builder() {
    return new AutoValue_Project.Builder();
  }

  private static GradleContext createContext(File directory, @Nullable Project parent) {
    // read the build file for the minSdkVersion
    File buildFile = new File(directory, BUILD_GRADLE);
    if (!buildFile.exists()) {
      throw new RuntimeException(
          "Invalid project directory with no gradle build file: " + buildFile.getAbsolutePath());
    }
    try {
      int defaultMinSdkVersion = parent != null ? parent.getContext().getMinSdkVersion() : 1;
      AndroidPluginVersion androidPluginVersion =
          parent != null ? parent.getContext().getAndroidPluginVersion() : null;
      String content = Files.asCharSource(buildFile, UTF_8).read();
      GradleContext.Builder builder =
          GroovyGradleParser.parseGradleBuildFile(
              content, defaultMinSdkVersion, androidPluginVersion);
      // try to read the manifest(s) in the project to determine if this project is for an onDemand
      // module.
      File manifestFile = new File(directory, MANIFEST);
      boolean isOnDemand = manifestFile.exists() && isOnDemand(manifestFile);
      builder.setOnDemand(isOnDemand);
      return builder.build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean isOnDemand(File manifestFile) {
    try (FileInputStream inputStream = new FileInputStream(manifestFile)) {
      XMLInputFactory factory = XMLInputFactory.newInstance();
      XMLEventReader eventReader = factory.createXMLEventReader(inputStream);

      while (eventReader.hasNext() && !eventReader.peek().isEndDocument()) {
        XMLEvent event = eventReader.nextTag();
        if (event.isStartElement()) {
          StartElement startElement = event.asStartElement();
          if (startElement.getName().getNamespaceURI().equals(DIST_URI)
              && Ascii.equalsIgnoreCase(startElement.getName().getLocalPart(), "module")) {
            Attribute onDemand = startElement.getAttributeByName(new QName(DIST_URI, "onDemand"));
            if (onDemand != null) {
              boolean isOnDemand =
                  onDemand.getValue().equals(VALUE_TRUE) || onDemand.getValue().equals(VALUE_1);
              eventReader.close();
              return isOnDemand;
            }
          }
        } else if (event.isEndElement()
            && Ascii.equalsIgnoreCase(((EndElement) event).getName().getLocalPart(), "manifest")) {
          break;
        }
      }
      eventReader.close();
    } catch (XMLStreamException | IOException e) {
      System.out.println(
          "Warning: Failed to parse android manifest " + manifestFile.getAbsolutePath());
      // assume that it is not an onDemand project rather than throwing an exception.
    }

    return false;
  }

  /** The {@link GradleContext} for this project. */
  public abstract GradleContext getContext();

  /** The file pointing to the initial project directory. */
  public abstract File getProjectDirectory();

  /** Builder for the {@link Project}. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Set the project context. */
    public abstract Builder setContext(GradleContext context);

    /** Set the project directory. */
    public abstract Builder setProjectDirectory(File projectDirectory);

    /** Build the project object. */
    public abstract Project build();
  }
}
