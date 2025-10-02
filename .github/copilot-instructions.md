# Copilot / AI Agent Instructions for aidemo1

Short purpose
- Small Spring Boot web service (artifactId: `aidemo1`). Primary entry: `com.example.aidemo1.Aidemo1Application`.

Big picture
- Single-module Maven Spring Boot app using `spring-boot-starter-web` and `spring-boot-starter-data-jpa`.
- Main class: `src/main/java/com/example/aidemo1/Aidemo1Application.java` which boots Spring context.
- Config lives in `src/main/resources/application.properties`.
- PostgreSQL database integration configured with environment variable overrides.
- Docker Compose setup includes PostgreSQL, Spring Boot app, and Adminer for database management.

Developer workflows (explicit commands)
- Build (uses wrapper): `./mvnw -DskipTests package` (Linux/macOS), or `mvnw.cmd -DskipTests package` on Windows.
- Run packaged JAR: `java -jar target/aidemo1-0.0.1-SNAPSHOT.jar` (default port 8080 unless overridden).
- Run tests: `./mvnw test`.
- **Docker workflows**: 
  - Start all services (PostgreSQL, app, Adminer): `docker compose up --build`
  - Stop services: `docker compose down`
  - Access Adminer at http://localhost:8081 (server: postgres, user/pass: postgres/postgres, db: aidemo)
- Fast compile+run during development: use VS Code run config `Launch Aidemo1Application (Spring Boot)` found in `.vscode/launch.json` (it launches `com.example.aidemo1.Aidemo1Application` and uses `projectName: aidemo1`).
- Remote debug (from terminal): build and run with JVM debug flags: `java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar target/aidemo1-0.0.1-SNAPSHOT.jar` and attach IDE to port 5005.

Project-specific conventions & patterns
- Java version declared in `pom.xml` as `<java.version>25</java.version>`; assume a JDK compatible with that value is required for local builds.
- Lombok is included as an optional dependency in the pom; annotation-processor configured in `maven-compiler-plugin`. The spring-boot repackage step explicitly excludes Lombok from the executable jar (`spring-boot-maven-plugin` excludes lombok). When editing code that uses Lombok, ensure IDE annotation processing is enabled.
- Artifact/project name is `aidemo1` (pom artifactId). The VS Code `projectName` in `launch.json` matches this; if renaming the module, update the launch config.
- .gitignore excludes `target/` and `.vscode/` — keep generated/build artifacts out of commits.
- **Spring Data Repository Query Methods**: Always prefer Spring Data Derived Query Methods over `@Query` annotations. Derived query methods (e.g., `findByUsername`, `existsByCode`, `deleteByName`) are automatically implemented by Spring Data from the method name. Only use `@Query` annotations when the query logic is too complex to express with a derived method name (e.g., complex joins, subqueries, or specific performance optimizations).

Integration & external dependencies
- Primary dependencies: `spring-boot-starter-web` (embedded web server + MVC), `spring-boot-starter-data-jpa` (JPA/Hibernate), `postgresql` driver.
- PostgreSQL database configured via environment variables (SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD) with sensible defaults for local dev.
- Docker Compose orchestrates three services: postgres (port 5432), app (port 8080), adminer (port 8081).
- No CI configuration detected. If you add CI, prefer using the Maven wrapper (`./mvnw`) to guarantee consistent Maven version.

Key files to reference
- `pom.xml` — dependency and build plugin decisions (Lombok handling, spring-boot repackage, PostgreSQL driver, JPA).
- `docker compose.yml` — orchestrates PostgreSQL, Spring Boot app, and Adminer containers.
- `Dockerfile` — multi-stage build for Spring Boot app (Maven build + JRE runtime).
- `src/main/java/com/example/aidemo1/Aidemo1Application.java` — application entrypoint.
- `src/main/resources/application.properties` — runtime configuration including database connection with env var overrides.
- `src/test/java/com/example/aidemo1/Aidemo1ApplicationTests.java` — example test scaffold.
- `.vscode/launch.json` — local debug/run config (created for this repo).
- `.mvn/wrapper/maven-wrapper.properties` and `mvnw` — use wrapper for reproducible builds.

Practical examples for agents (do this, not generic advice)
- To add a new REST controller, follow package `com.example.aidemo1` and put controllers in `src/main/java/com/example/aidemo1/web` or `.../controller`. Keep class-level `@RestController` and map under `/` or `/api`.
- When changing dependencies or Java source level, update `pom.xml` and run `./mvnw -DskipTests package` to validate repackaging (spring-boot-maven-plugin runs during package).
- If you add Lombok annotations, ensure `pom.xml` still keeps Lombok as optional (as currently done) and inform reviewers to enable annotation processing in IDE.

Editing and testing tips for agents
- Small edits: run `./mvnw -DskipTests package` to catch compile/annotation processing issues.
- Run the app locally via VS Code launch or `java -jar` after packaging. Integration tests will likely use random ports only if configured — current test class only checks context load.

Git and version control rules
- **NEVER commit or push changes unless explicitly asked by the user**. Always wait for clear instruction to commit (e.g., "commit these changes", "push to git", "create a commit").
- When making changes, stage them and show what was modified, but do NOT automatically commit.
- If implementing a feature or fix, complete the work and verify it builds/runs, then inform the user and ask if they want to commit.
- Only use commit messages with issue references (e.g., "Closes #N") when the user explicitly requests closing an issue via commit.

If anything in this file looks incomplete or you want more detail about CI, runtime properties, or example endpoints, tell me which area to expand and I will iterate.