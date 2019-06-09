# Size Analyzer

The Size Analyzer is a tool for developers to understand the size of their
Android application.

## How to build the size analyzer
The tool can be built using [gradle](https://gradle.org/). An executable jar can
be built using the command below:

``` shell
./gradlew executableJar
```

## How to use the size analyzer

The executable jar can be run against either an Android Studio project or an
[Android App Bundle](https://g.co/androidappbundle).

```shell
java -jar build/libs/analyzer.jar check-bundle <path-to-aab>
java -jar build/libs/analyzer.jar check-project <path-to-project-directory>
```

## Binary distributions
Pre-built distributions of this tool will be made available with each release
on our [releases page](https://github.com/android/size-analyzer/releases).
