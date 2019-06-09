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

package com.android.tools.sizereduction.analyzer.model;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.control.CompilePhase;

/**
 * This class parses a build.gradle file. It currently extracts the minSdkVersion along with the
 * proguard configurations for each buildType. In addition, it recognizes if the build.gradle file
 * is for an android application, dynamic-feature, or other build.gradle type. It can be extended to
 * add other dsl properties by modifying the checkDslPropertyAssignment function. For property
 * values that are defined in other build.gradle files, the value will just be the string
 * representation as it cannot evaluate the value.
 */
public final class GroovyGradleParser extends CodeVisitorSupport {

  private final List<MethodCallExpression> methodCallStack = new ArrayList<>();
  private int minSdkVersion = -1;
  private final Map<String, ProguardConfig.Builder> proguardConfigs = new HashMap<>();
  private final GradleContext.Builder gradleContextBuilder;
  private BundleConfig.Builder bundleConfigBuilder = BundleConfig.builder();
  private final String content;
  private int defaultMinSdkVersion = 1;

  private GroovyGradleParser(
      String content, int defaultMinSdkVersion, AndroidPluginVersion defaultAndroidPluginVersion) {
    this.content = content;
    this.defaultMinSdkVersion = defaultMinSdkVersion;
    this.gradleContextBuilder =
        GradleContext.builder().setAndroidPluginVersion(defaultAndroidPluginVersion);
  }

  public static GradleContext.Builder parseGradleBuildFile(
      String content,
      int defaultMinSdkVersion,
      @Nullable AndroidPluginVersion defaultAndroidPluginVersion) {
    // We need to have an abstract syntax tree, which is what the conversion phase produces,
    // Anything more will try to semantically understand the groovy code.
    List<ASTNode> astNodes = new AstBuilder().buildFromString(CompilePhase.CONVERSION, content);
    GroovyGradleParser parser =
        new GroovyGradleParser(content, defaultMinSdkVersion, defaultAndroidPluginVersion);

    for (ASTNode node : astNodes) {
      if (node instanceof ClassNode) {
        // class nodes do not implement the visit method, and will throw a runtime exception.
        continue;
      }
      node.visit(parser);
    }
    return parser.getGradleContextBuilder();
  }

  public GradleContext.Builder getGradleContextBuilder() {
    ImmutableMap<String, ProguardConfig> configs =
        proguardConfigs.entrySet().stream()
            .collect(toImmutableMap(entry -> entry.getKey(), entry -> entry.getValue().build()));
    gradleContextBuilder
        .setProguardConfigs(configs)
        .setBundleConfig(bundleConfigBuilder.build())
        .setMinSdkVersion(minSdkVersion > 0 ? minSdkVersion : defaultMinSdkVersion);
    return gradleContextBuilder;
  }

  @Override
  public void visitMethodCallExpression(MethodCallExpression expression) {
    // get initial parent, parentParent values.
    String parent =
        methodCallStack.size() < 1 ? "" : Iterables.getLast(methodCallStack).getMethodAsString();
    String parentParent = getParentParent();
    methodCallStack.add(expression);

    if (expression.getArguments() instanceof ArgumentListExpression) {
      Expression objectExpression = expression.getObjectExpression();
      String newParent = getValidParentString(objectExpression);
      if (newParent != null) {
        parentParent = parent;
        parent = newParent;
        if (objectExpression instanceof PropertyExpression) {
          String newParentParent =
              getValidParentString(((PropertyExpression) objectExpression).getObjectExpression());
          if (newParentParent != null) {
            parentParent = newParentParent;
          }
        }
      }
      checkDslProperty(expression, parent, parentParent);
    }
    super.visitMethodCallExpression(expression);
    methodCallStack.remove(methodCallStack.size() - 1);
  }

  @Override
  public void visitTupleExpression(TupleExpression tupleExpression) {
    if (!methodCallStack.isEmpty()) {
      MethodCallExpression call = Iterables.getLast(methodCallStack);
      if (call.getArguments() == tupleExpression) {
        String parent = call.getMethodAsString();
        String parentParent = getParentParent();
        if (!(tupleExpression instanceof ArgumentListExpression)) {
          Map<String, String> namedArguments = new HashMap<>();
          for (Expression subExpr : tupleExpression.getExpressions()) {
            if (subExpr instanceof NamedArgumentListExpression) {
              NamedArgumentListExpression nale = (NamedArgumentListExpression) subExpr;
              for (MapEntryExpression mae : nale.getMapEntryExpressions()) {
                namedArguments.put(
                    mae.getKeyExpression().getText(), mae.getValueExpression().getText());
              }
            }
          }
          checkMethodCall(parent, parentParent, namedArguments);
        }
      }
    }
    super.visitTupleExpression(tupleExpression);
  }

  /** Handles a groovy BinaryExpression such as foo = true, or bar.baz.foo = true. */
  @Override
  public void visitBinaryExpression(BinaryExpression binaryExpression) {
    if (!methodCallStack.isEmpty()) {
      MethodCallExpression call = Iterables.getLast(methodCallStack);
      String parent = call.getMethodAsString();
      String parentParent = getParentParent();
      Expression leftExpression = binaryExpression.getLeftExpression();
      Expression rightExpression = binaryExpression.getRightExpression();
      if (rightExpression instanceof ConstantExpression
          && (leftExpression instanceof PropertyExpression
              || leftExpression instanceof VariableExpression)) {
        String value = rightExpression.getText();
        String property = "";
        if (leftExpression instanceof PropertyExpression) {
          Expression leftPropertyExpression = ((PropertyExpression) leftExpression).getProperty();
          if (!(leftPropertyExpression instanceof ConstantExpression)) {
            return;
          }
          property = ((ConstantExpression) leftPropertyExpression).getText();
          Expression leftObjectExpression =
              ((PropertyExpression) leftExpression).getObjectExpression();
          parentParent = parent;
          parent = getValidParentString(leftObjectExpression);
          if (leftObjectExpression instanceof PropertyExpression) {
            parentParent =
                getValidParentString(
                    ((PropertyExpression) leftObjectExpression).getObjectExpression());
          }
        } else {
          property = ((VariableExpression) leftExpression).getName();
        }
        checkDslPropertyAssignment(property, value, parent, parentParent);
      }
    }
    super.visitBinaryExpression(binaryExpression);
  }

  /**
   * This returns a String representation of the object if it is a valid expression for a parent
   * object. Otherwise, it will return null. For instance in `defaultConfig.minSdkVersion 14`,
   * defaultConfig would be a valid parent expression.
   */
  private String getValidParentString(Expression objectExpression) {
    if (objectExpression instanceof PropertyExpression) {
      return ((PropertyExpression) objectExpression).getPropertyAsString();
    } else if (objectExpression instanceof VariableExpression) {
      VariableExpression variableExpression = (VariableExpression) objectExpression;
      if (!variableExpression.getName().equals("this")) {
        return variableExpression.getName();
      }
    }
    return null;
  }

  /**
   * This will return an initial guess as to the string representation of the parent parent object,
   * based solely on the method callstack hierarchy. Any direct property or variable parents should
   * be resolved by using the getValidStringRepresentation function.
   */
  private String getParentParent() {
    for (int i = methodCallStack.size() - 2; i >= 0; i--) {
      MethodCallExpression expression = methodCallStack.get(i);
      Expression arguments = expression.getArguments();
      if (arguments instanceof ArgumentListExpression) {
        ArgumentListExpression ale = (ArgumentListExpression) arguments;
        List<Expression> expressions = ale.getExpressions();
        if (expressions.size() == 1 && expressions.get(0) instanceof ClosureExpression) {
          return expression.getMethodAsString();
        }
      }
    }

    return null;
  }

  /**
   * This will evaluate the dsl property assignment, to store the valid value. In
   * android.defaultConfig.minSdkVersion 15, "minSdkVersion 15" is the methodCall, defaultConfig is
   * the parent, and android is the parentParent expression. This can also be written as android {
   * defaultConfig { minSdkVersion 15 } } in the build.gradle file as well.
   *
   * @param call is a the method call expression.
   * @param parent is the string representation for the parent object.
   * @param parentParent is the string representation for the parent of the parent object.
   */
  private void checkDslProperty(MethodCallExpression call, String parent, String parentParent) {
    String property = call.getMethodAsString();
    if (property == null) {
      return;
    }
    String value = getText(call.getArguments(), content);
    checkDslPropertyAssignment(property, value, parent, parentParent);
  }

  /**
   * This will evaluate the dsl property assignment, to store the valid value. In
   * android.defaultConfig.minSdkVersion 15, "minSdkVersion" is the property, 15 is the value,
   * defaultConfig is the parent, and android is the parentParent expression. This can also be
   * written as android { defaultConfig { minSdkVersion 15 } } in the build.gradle file as well.
   *
   * @param property is the property being assigned.
   * @param value is the value for the dsl assignment.
   * @param parent is the string representation for the parent of the property being assigned.
   * @param parentParent is the string representation for the parent of the parent object.
   */
  private void checkDslPropertyAssignment(
      String property, String value, String parent, String parentParent) {
    String buildType =
        "buildTypes".equals(parentParent) ? parent : ProguardConfig.DEFAULT_CONFIG_NAME;
    ProguardConfig.Builder proguardConfig =
        proguardConfigs.containsKey(buildType)
            ? proguardConfigs.get(buildType)
            : ProguardConfig.builder();
    switch (property) {
      case "minSdkVersion":
        int curMinSdkVersion = getSdkVersion(value);
        minSdkVersion =
            (minSdkVersion > 0 && minSdkVersion < curMinSdkVersion)
                ? minSdkVersion
                : curMinSdkVersion;
        break;
      case "minifyEnabled":
        proguardConfig.setMinifyEnabled(value.equals("true"));
        proguardConfigs.put(buildType, proguardConfig);
        break;
      case "proguardFiles":
        proguardConfig.setHasProguardRules(true);
        proguardConfigs.put(buildType, proguardConfig);
        break;
      case "useProguard":
        // useProguard may use proguard or r8, but this effectively controls whether
        // obfuscation is enabled for this project.
        proguardConfig.setObfuscationEnabled(value.equals("true"));
        proguardConfigs.put(buildType, proguardConfig);
        break;
      case "enableSplit":
        if (parentParent.equals("bundle")) {
          switch (parent) {
            case "abi": // 3.2 and above.
              bundleConfigBuilder.setAbiSplitEnabled(value.equals("true"));
              break;
            case "density":
              bundleConfigBuilder.setDensitySplitEnabled(value.equals("true"));
              break;
            case "language":
              bundleConfigBuilder.setLanguageSplitEnabled(value.equals("true"));
              break;
            default:
              // ignore other proprties
              break;
          }
        }
        break;
      case "classpath":
        if (parent.equals("dependencies") && parentParent.equals("buildscript")) {
          if (isStringLiteral(value)) {
            String classPath = getStringLiteralValue(value);
            if (classPath.startsWith("com.android.tools.build:gradle:")) {
              String version = classPath.substring("com.android.tools.build:gradle:".length());
              gradleContextBuilder.setAndroidPluginVersion(AndroidPluginVersion.create(version));
            }
          }
        }
        break;
      default:
        // there are many other valid properties, but we do not care to store them yet.
        break;
    }
  }

  private void checkMethodCall(
      String statement, String parent, Map<String, String> namedArguments) {
    if (statement.equals("apply") && parent == null && namedArguments.containsKey("plugin")) {
      String plugin = namedArguments.get("plugin");
      switch (plugin) {
        case "com.android.application":
          gradleContextBuilder.setPluginType(GradleContext.PluginType.APPLICATION);
          break;
        case "com.android.dynamic-feature":
          gradleContextBuilder.setPluginType(GradleContext.PluginType.DYNAMIC_FEATURE);
          break;
        case "com.android.feature":
          gradleContextBuilder.setPluginType(GradleContext.PluginType.FEATURE);
          break;
        default:
          // there are other plugins that can be applied, ignore them.
          break;
      }
    }
  }

  private static String getText(ASTNode node, String content) {
    Offsets offset = getTextOffsets(node, content);
    return content.substring(offset.getStart(), offset.getEnd());
  }

  private static Offsets getTextOffsets(ASTNode node, String content) {
    if (node.getLastLineNumber() == -1 && node instanceof TupleExpression) {
      // Workaround: TupleExpressions yield bogus offsets, so use its
      // children instead
      TupleExpression exp = (TupleExpression) node;
      List<Expression> expressions = exp.getExpressions();
      if (!expressions.isEmpty()) {
        return Offsets.create(
            getTextOffsets(expressions.get(0), content).getStart(),
            getTextOffsets(expressions.get(expressions.size() - 1), content).getEnd());
      }
    }

    if (node instanceof ArgumentListExpression) {
      List<Expression> expressions = ((ArgumentListExpression) node).getExpressions();
      if (expressions.size() == 1) {
        return getTextOffsets(expressions.get(0), content);
      }
    }

    int start = 0;
    int end = content.length();
    int line = 1;
    int startLine = node.getLineNumber();
    int startColumn = node.getColumnNumber();
    int endLine = node.getLastLineNumber();
    int endColumn = node.getLastColumnNumber();
    int column = 1;
    for (int index = 0, len = end; index < len; index++) {
      if (line == startLine && column == startColumn) {
        start = index;
      }
      if (line == endLine && column == endColumn) {
        end = index;
        break;
      }

      char c = content.charAt(index);
      if (c == '\n') {
        line++;
        column = 1;
      } else {
        column++;
      }
    }
    return Offsets.create(start, end);
  }

  private int getSdkVersion(String value) {
    int version = defaultMinSdkVersion;
    if (isStringLiteral(value)) {
      String codeName = getStringLiteralValue(value);
      if (codeName != null) {
        if (isNumberString(codeName)) {
          return getIntLiteralValue(codeName, defaultMinSdkVersion);
        }
      }
    } else {
      version = getIntLiteralValue(value, defaultMinSdkVersion);
    }
    return version;
  }

  private static boolean isNumberString(String token) {
    if (token == null || token.isEmpty()) {
      return false;
    }
    for (int index = 0; index < token.length(); ++index) {
      if (!Character.isDigit(token.charAt(index))) {
        return false;
      }
    }
    return true;
  }

  private static boolean isStringLiteral(String token) {
    return (token.startsWith("\"") && token.endsWith("\""))
        || (token.startsWith("'") && token.endsWith("'"));
  }

  private static String getStringLiteralValue(String value) {
    if (value.length() > 2
        && ((value.startsWith("'") && value.endsWith("'"))
            || (value.startsWith("\"") && value.endsWith("\"")))) {
      return value.substring(1, value.length() - 1);
    }
    return null;
  }

  private static int getIntLiteralValue(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /** The start and end string offsets, for a valid substring within the content being parsed. */
  @AutoValue
  abstract static class Offsets {

    public static Offsets create(int start, int end) {
      return new AutoValue_GroovyGradleParser_Offsets.Builder().setStart(start).setEnd(end).build();
    }

    public abstract int getStart();

    public abstract int getEnd();

    /** Builder for the {@link Offsets}. */
    @AutoValue.Builder
    abstract static class Builder {

      public abstract Builder setStart(int start);

      public abstract Builder setEnd(int end);

      public abstract Offsets build();
    }
  }
}
