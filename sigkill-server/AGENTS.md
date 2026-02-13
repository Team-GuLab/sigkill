# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/gulab/sigkillserver`: main application code.
  - `config/`: security and STOMP/WebSocket configuration.
  - `common/`: shared response wrappers, base entities, and exception handlers.
  - `domain/user` and `domain/room`: domain modules with `controller`, `service`, `repository`, `model`, `dto`, and `exception` packages.
- `src/main/resources/application.yml`: runtime configuration (session and Redis settings).
- `src/test/java/com/gulab/sigkillserver`: service and application tests.
- `docs/`: coding/test style guides and STOMP specifications.

## Build, Test, and Development Commands
- `./gradlew bootRun`: start the server locally.
- `./gradlew test`: run all tests (JUnit 5).
- `./gradlew test --tests "com.gulab.sigkillserver.domain.room.service.RoomServiceTest"`: run a single test class.
- `./gradlew clean build`: clean, compile, test, and package.
- Swagger (after startup): `http://localhost:8080/swagger-ui/index.html`.

## Coding Style & Naming Conventions
- Java 21, 4-space indentation, and no wildcard imports.
- Keep package-by-domain boundaries; put shared logic under `common`.
- Naming rules:
  - `*Controller`, `*Service`, `*Repository` for layers.
  - DTOs as `*Request`, `*Response`, and domain-specific `dto/stomp/*` for WebSocket payloads.
  - Domain error enums as `*ErrorCode`; throw `CustomException` for business errors.
- Prefer immutable DTOs (`record`) and builder/constructor-based model creation.

## Testing Guidelines
- Use JUnit 5 + Spring Boot Test + AssertJ.
- Structure tests with `@Nested` and explicit `Given/When/Then` comments.
- Test names should be descriptive with underscore-separated phrases (Korean naming is used in this repository).
- Cover happy paths, error paths, and boundary cases; aim for very high coverage on critical business logic.

## Commit & Pull Request Guidelines
- Follow observed commit format: `feat|fix|refactor|test|docs|chore: #<issue> <summary>`.
  - Example: `refactor: #8 RoomService 메서드의 sessionId를 userId로 변경`.
- PRs should include:
  - concise change summary and rationale,
  - linked issue (for example, `#8`),
  - test evidence (key `./gradlew test` results),
  - API/WebSocket request-response examples when behavior changes.
