# buildSrc Agent Context

## Gradle Convention Plugin

`buildSrc/src/main/groovy/parent.gradle` is the shared Gradle convention used by
the report subprojects. It applies Java, Java Library, and Maven Publish
plugins, sets the Java 11 toolchain, configures common repositories, and defines
shared compile and test dependencies.

## Shared Build Tasks

The `parent` plugin defines `buildZip` for Lambda packaging by bundling compiled
classes, resources, and runtime dependencies under `lib`. It also defines
`copyDependencies`, which copies runtime dependencies to `build/dependencies`
and is wired into `build`.

## Dependency/Test Defaults

Common dependencies include Lombok, AWS Lambda runtime APIs, POI, SLF4J,
Logback, Commons Lang, Commons IO, and `restapi-bean`. Tests inherit JUnit 5,
AssertJ, Mockito, and JUnit Platform execution.

## Core Files

- `buildSrc/build.gradle`: enables the Groovy Gradle plugin build.
- `buildSrc/src/main/groovy/parent.gradle`: shared subproject convention.
