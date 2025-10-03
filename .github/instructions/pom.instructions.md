---
applyTo: 'pom.xml'
---
NEVER EVER downgrade the version of a dependency in the `pom.xml` file. If you need to change a dependency version, always upgrade it to a newer version.
NEVER EVER change the version of a dependency to a SNAPSHOT version in the `pom.xml` file. If you need to change a dependency version, always upgrade it to a newer stable version.
NEVER EVER change the version of a dependency to a version that is not released yet in the `pom.xml` file. If you need to change a dependency version, always upgrade it to a released stable version.
NEVER EVER change java version in the `pom.xml` file. It was decided to use Java 25, and in case any library does not support it, this library should be replaced with another one that supports Java 25.