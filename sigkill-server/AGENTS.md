# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/gulab/sigkillserver`: main application code.
  - `config/`: security and STOMP/WebSocket configuration.
  - `common/`: shared response wrappers, base entities, and exception handlers.
  - `domain/user` and `domain/room`: domain modules with `controller`, `service`, `repository`, `model`, `dto`, and `exception` packages.
- `src/main/resources/application.yml`: runtime configuration (session and Redis settings).
- `src/test/java/com/gulab/sigkillserver`: service and application tests.
- `docs/`: coding/test style guides and STOMP specification.
  - `docs/STOMP_MESSAGE_SPEC.md`: STOMP 단일 계약 문서 (Single Source of Truth)

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

## STOMP Documentation Rule
- STOMP 경로(`@MessageMapping`), 토픽/큐 목적지, 이벤트 type/필드, 에러 코드 계약이 바뀌면 같은 PR에서 `docs/STOMP_MESSAGE_SPEC.md`를 함께 수정한다.
- STOMP 문서는 단일 파일로 유지한다. 중복 가이드 문서는 추가하지 않는다.

## Game Domain Working Memory
- 게임 시작 시 `Game`, `GamePlayer`를 생성한다. `GamePlayer`는 `score=0`, `status=ALIVE`로 시작한다.
- `Game`은 문제 순서를 `quizIds`(불변 리스트)로 가진다. 생성자에서 `List.copyOf(...)`로 고정한다.
- 라운드 시작 시 `currentQuizIndex`를 증가시키고, `roundStartedAtMillis = Instant.now().toEpochMilli()`로 기록한다.
- 라운드 제한시간은 `Game` 클래스 상수 `ROUND_COUNTDOWN_MILLIS = 5_000L`를 사용한다.
- 제출은 제한시간 내 요청만 처리하며, 유저별 마지막 제출을 최종 제출로 간주한다.
- 라운드 종료 시 정답자는 `+1점`, 오답/미제출자는 `DEAD` 처리한다.
- 종료 조건은 `전원 사망`, `1명 생존`, `모든 문제 소진` 중 하나를 만족할 때다.
- 게임 종료 후 결과를 `score` 기준으로 노출하고, `Game`, `GamePlayer`, `SelectedChoice`를 메모리 저장소에서 삭제한다.
- 종료 후 또는 마감 후에 늦게 도착한 제출은 `status`, `quizId`, `deadline` 검증에서 무시한다.
