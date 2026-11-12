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

| Module       | Parent | Depends on                                                          |
|--------------|--------|---------------------------------------------------------------------|
| `model-impl` | `core` | `patmal.workshops:model`                                            |
| `logic-impl` | `core` | `model-impl`, `patmal.workshops:logic`, `patmal.workshops:model`    |
| `app`        | `root` | `logic-impl`                                                        |

All modules use the groupId `patmal.workshops.student.impl` and the version
`1.0-SNAPSHOT`, both declared once in the root `pom.xml`. Every other version
(the API jars, the tester, the plugins) is a property in the root `pom.xml`, and
the child modules never repeat a version.

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
mvn clean compile
```

This builds all the modules, in dependency order: `root`, `core`, `model-impl`,
`logic-impl`, `app`.
