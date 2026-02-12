# Repository Guidelines

## Project Structure & Module Organization
- Spring Boot Java 21 project. Build scripts in `build.gradle` and `settings.gradle`; helper docs live in `docs/` (coding and test style guides, STOMP specs).
- Source resides in `src/main/java/com/gulab/sigkillserver/` with `config/` (websocket, security, redis), `common/` (base entities, responses, exception handling), and domain packages (`room`, `quiz`, `user`, `game`) each containing `controller`, `service`, `repository`, `dto`, `model`, and `exception` subpackages.
- Application resources are under `src/main/resources/` (`application.yml`, `static/`, `templates/`). Tests sit in `src/test/java/com/gulab/sigkillserver/`, organized by domain service.

## Build, Test, and Development Commands
- `./gradlew clean build` — compile and run all checks, producing an executable JAR.
- `./gradlew test` — execute JUnit 5 test suite with AssertJ and Spring Security test support.
- `./gradlew bootRun` — start the API/WebSocket server locally; ensure Redis (localhost:6379) is running.
- `./gradlew bootJar` — build only the application JAR for deployment.

## Coding Style & Naming Conventions
- Follow `docs/CODING_STYLE_GUIDE.md`: 4-space indentation, Lombok for boilerplate, constructors + builders for entities, and clean separation by layer/domain.
- Class naming patterns: `{Domain}Controller`, `{Domain}Service`, `{Domain}Repository`, DTOs as `{Noun}Request`/`{Noun}Response`, exceptions grouped by domain-specific error codes.
- Prefer records for simple DTOs, `@RequiredArgsConstructor` for dependency injection, and `BaseResponse` for consistent API payloads. Keep business logic in services; controllers remain thin.

## Testing Guidelines
- Tests use JUnit 5 with AssertJ fluent assertions; Spring Boot test slices where possible. Refer to `docs/TEST_STYLE_GUIDE.md` for structure.
- Name test methods descriptively in Korean with underscores (e.g., `이메일로_회원_조회에_성공한다`). Use `@Nested` classes per feature and Given/When/Then comments to document intent.
- Place fixtures/helpers near the test class; keep tests isolated and independent of order.

## Commit & Pull Request Guidelines
- Commit messages follow `type: #issue summary` (e.g., `refactor: #8 RoomService 메서드 개선`). Keep commits scoped and reversible.
- PRs should describe the change, link related issues, note migration/config impacts (e.g., Redis, session cookies), and include screenshots or OpenAPI samples for user-facing updates.

## Security & Configuration Tips
- Configure secrets and environment-specific values via environment variables or profiles; avoid committing credentials. Update `application.yml` for prod-safe settings (e.g., `server.servlet.session.cookie.secure=true`).
- WebSocket/STOMP behavior and message schemas are documented in `docs/STOMP_GUIDE.md` and `docs/STOMP_MESSAGE_SPEC.md`; keep changes aligned with those specs.
