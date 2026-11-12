# Workshop Registration System

A multi-module Maven project that implements the `patmal.workshops` API 2.0
(EX2 – Maven Evolution).

## Requirements

- JDK 25
- Maven 3.9 or later

## Project structure

```
root                    aggregator + top parent          (pom)
|-- core                aggregator + parent of the impls (pom)
|   |-- model-impl      implementation of the model interfaces  (jar)
|   `-- logic-impl      implementation of WorkshopSystem       (jar)
`-- app                 the runnable application, workshop-cli (jar)
```

| Module       | Parent | Depends on                                                                                     |
|--------------|--------|------------------------------------------------------------------------------------------------|
| `model-impl` | `core` | `patmal.workshops:model`                                                                       |
| `logic-impl` | `core` | `model-impl`, `patmal.workshops:logic`, `patmal.workshops:model`, the libraries below          |
| `app`        | `root` | `logic-impl` (plus the APIs it uses directly), `patmal.workshops:workshop-tester` (test scope) |

All modules use the groupId `patmal.workshops.student.impl` and the version
`1.0-SNAPSHOT`, both declared once in the root `pom.xml`. Every other version
(the API jars, the tester, the libraries, the plugins) is a property in the root
`pom.xml`, managed in its `<dependencyManagement>` / `<pluginManagement>`, and the
child modules never repeat a version.

### Libraries

| Library                                          | Used for                          | Scope in `logic-impl` |
|--------------------------------------------------|-----------------------------------|-----------------------|
| `org.apache.logging.log4j:log4j-api`             | logging (the code uses the API)   | compile               |
| `org.apache.logging.log4j:log4j-core`            | logging (the implementation)      | runtime               |
| `commons-validator:commons-validator`            | e-mail validation                 | compile               |
| `com.fasterxml.jackson.core:jackson-databind`    | the three JSON exports            | compile               |
| `org.apache.commons:commons-csv`                 | the CSV import (bonus)            | compile               |

`commons-codec` is not used directly. It is brought in by `commons-csv` and is
managed in the root pom to override the vulnerable version that `commons-csv`
brings (CVE-2026-119078). Run `mvn dependency:tree` to see every transitive
dependency and which library brought it.

## One-time setup: install the API jars into the local Maven repository

The API jars (`model-2.0.jar`, `logic-2.0.jar`) and the tester
(`workshop-tester-2.0.jar`) are not published on Maven Central, so they must be
installed into the local repository (`~/.m2/repository`) once on every computer
that builds the project.

1. Copy the three jars received with the exercise into a `lib/` folder in the
   root of the project (the folder is ignored by git):

   ```
   lib/model-2.0.jar
   lib/logic-2.0.jar
   lib/workshop-tester-2.0.jar
   ```

2. From the root of the project, run these exact commands:

   ```shell
   mvn install:install-file "-Dfile=lib/model-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=model" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"
   mvn install:install-file "-Dfile=lib/logic-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=logic" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"
   mvn install:install-file "-Dfile=lib/workshop-tester-2.0.jar" "-DgroupId=patmal.workshops" "-DartifactId=workshop-tester" "-Dversion=2.0" "-Dpackaging=jar" "-DgeneratePom=true"
   ```

Notes on the commands:

- **The `-D...` arguments are quoted** so the commands work unchanged in
  PowerShell, cmd and bash. Without the quotes, PowerShell can split an argument
  such as `-Dversion=2.0` at the dot.
- **Keep `-DgeneratePom=true`.** Each jar contains an embedded `pom.xml`
  whose parent (`patmal.workshops:workshops-aggregator:1.0-SNAPSHOT`) does not
  exist, and the tester's embedded pom also uses a different artifactId (`main`).
  Without the flag, maven-install-plugin 3.0.x–3.1.x (the version that Maven 3.9.x
  uses by default) installs that embedded pom. Every build that uses the jar then
  fails with `Failed to read artifact descriptor ... workshops-aggregator:pom:1.0-SNAPSHOT`.
  With the flag, every plugin version generates a minimal, clean pom for the
  coordinates above.
- Because the generated poms declare no dependencies, Maven does not know that
  `logic` uses `model`. Every module that uses `logic` therefore declares `model`
  explicitly as well.

To check the installation:

```shell
ls ~/.m2/repository/patmal/workshops/model/2.0 ~/.m2/repository/patmal/workshops/logic/2.0 ~/.m2/repository/patmal/workshops/workshop-tester/2.0
```

## Build

From the root of the project:

```shell
mvn clean install
```

This command:

1. Builds all the modules in dependency order: `root`, `core`, `model-impl`,
   `logic-impl`, `app`.
2. Packs the uber jar **`app/target/workshop-cli.jar`** in the `package` phase.
   The jar holds the application, our modules and all their compile and runtime
   dependencies, and declares its main class.
3. Runs the workshop tester in the `verify` phase (bonus 8.2). A failing tester
   fails the build, so nothing is installed.

To build without running the tester, add `"-Dexec.skip"`.

## Run the application

From the root of the project:

```shell
java -jar app/target/workshop-cli.jar students.csv
```

The application fills a `WorkshopSystem` with demo data: two instructors, three
workshops, the students of the CSV file, and teams of two. At least one student
stays free. It then writes `instructors.json`, `students.json` and `teams.json`
to the current folder; git ignores these three files.

- The argument is a UTF-8 CSV file with the header `id,name,email`. `students.csv`
  in the project root is a sample.
- Without an argument, the application imports the same demo students from the
  `students.csv` bundled in the jar:

  ```shell
  java -jar app/target/workshop-cli.jar
  ```

- The application logs every step at INFO level and every rejected operation at
  WARN level. The configuration is `app/src/main/resources/log4j2.xml`. To see only
  the warnings:

  ```shell
  java "-Dworkshops.log.level=warn" -jar app/target/workshop-cli.jar students.csv
  ```

## Check the implementation with the tester

The uber jar contains our implementation and the API classes. The tester needs
nothing else. From the root of the project (on Linux and macOS, use `:` instead
of `;`):

```shell
java -cp "lib/workshop-tester-2.0.jar;app/target/workshop-cli.jar" patmal.workshops.main.Main
```

The run must end with `RESULT: ALL OK`. The run also prints a few hundred log
lines: every operation of the tester is logged. Add `"-Dworkshops.log.level=warn"`
after `java` to see only the warnings.

## Notes on the build

- **Uber jar (maven-shade-plugin).** The configuration does four things:
  - It sets `Main-Class` in the manifest.
  - It sets `Multi-Release: true`. Log4j, Jackson and the Commons libraries are
    multi-release jars, and without this attribute the JVM silently loads the
    fallback classes for older Java versions of these libraries instead of the
    versions meant for newer Java.
  - It merges the `META-INF/services` files and the `NOTICE` files of the
    libraries.
  - It keeps a single copy of the license text, because every library is under
    the Apache License 2.0. It also leaves out the libraries' `module-info.class`
    files and signatures.

  Only `log4j-core` contains a Log4j plugin cache (`Log4j2Plugins.dat`), so no
  plugin-cache merging is needed. Test dependencies, the tester among them, are
  never packed into the jar.
- **Tester in the build (exec-maven-plugin).** The build uses the `exec` goal,
  which runs the tester in a separate JVM with the test class path of `app`. The
  `java` goal does not work for this tester, because it runs the tester inside
  Maven's own JVM:
  - The tester looks for the implementation on `java.class.path`, which there
    contains only Maven's launcher.
  - The tester ends with `System.exit()`, which would stop Maven itself.
