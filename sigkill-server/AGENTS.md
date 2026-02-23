# Repository Guidelines

## Project Overview
실시간 퀴즈 게임 플랫폼 서버.

- 기술 스택: Spring Boot 3.5.10, Java 21, Spring WebSocket, Spring Security
- 인증: 세션 기반 게스트 인증
- 실시간 통신: STOMP over WebSocket
- 데이터 저장: 인메모리 저장소(휘발성)

## Project Structure & Module Organization
- `src/main/java/com/gulab/sigkillserver`: 메인 애플리케이션 코드
  - `config/`: Security/STOMP/WebSocket 설정
  - `common/`: 공통 응답, BaseEntity, 전역 예외 처리
  - `domain/user`: 사용자 도메인
  - `domain/room`: 방 도메인
  - `domain/game`: 게임 도메인
- `src/main/resources/application.yml`: 런타임 설정
- `src/test/java/com/gulab/sigkillserver`: 테스트 코드
- `docs/STOMP_MESSAGE_SPEC.md`: STOMP 단일 계약 문서(SSOT)

## Build, Test, and Development Commands
- `./gradlew bootRun`: 로컬 서버 실행
- `./gradlew test`: 전체 테스트 실행
- `./gradlew test --tests "com.gulab.sigkillserver.domain.room.service.RoomServiceTest"`: 단일 테스트 클래스 실행
- `./gradlew clean build`: 클린/빌드/테스트/패키징
- Swagger: `http://localhost:8080/swagger-ui/index.html`

## Architecture Notes
### 인증 및 보안
- `Spring Security + Session` 기반.
- 비로그인 사용자는 게스트 로그인으로 세션 생성 후 접근.
- `SecurityConfig` 기준:
  - CSRF 비활성화, CORS 허용
  - `/api/**`는 `ROLE_GUEST` 필요
  - `/api/v1/users/guest-login`, `/ws/**`, Swagger는 인증 없이 접근 가능

### 세션 인증 흐름
1. `POST /api/v1/users/guest-login`
2. 세션 ID 기반 사용자 조회/생성
3. 신규 사용자면 닉네임 생성
4. `Authentication`을 `SecurityContext` 및 세션에 저장
5. 이후 요청에서 `Principal.getName()`으로 세션 ID 조회

### 인메모리 저장소
- 저장소는 `ConcurrentHashMap` 기반
- 서버 재시작 시 데이터 초기화
- 상태 일관성이 필요한 게임 처리(제출/라운드 종료)는 `gameId` 단위 임계영역에서 처리

## Coding Style & Naming Conventions
- Java 21, 4-space indentation, wildcard import 금지
- 도메인 경계 유지, 공통 로직은 `common`에 배치
- 클래스 명명:
  - `*Controller`, `*Service`, `*Repository`
  - DTO: `*Request`, `*Response`, STOMP payload는 `dto/stomp/*`
  - 에러 enum: `*ErrorCode`, 비즈니스 예외는 `CustomException`
- DTO는 가능한 `record` 등 불변 모델 선호

## Logging Policy
- Use channel-based MDC for log separation:
  - REST: `channel=REST` via `RestMdcFilter`
  - WebSocket/STOMP: `channel=WS` via `StompHandler` and `StompEventListener`
- Use metrics-first for performance measurement:
  - REST RPS/RPM: use Actuator `http.server.requests` metrics
  - WS traffic: use `sigkill.stomp.frames.total{command=...}` counter
  - Prometheus scrape endpoint: `/actuator/prometheus`
- Do not emit per-request `INFO` logs from REST controllers for polling/read APIs.
- Log level guideline:
  - Success: `INFO` only for state-changing operations (create/join/leave/ready/start/end)
  - Validation/business failure (`CustomException`, access denied): `WARN`
  - Unexpected runtime failure: `ERROR`
  - Polling/read success traces: `DEBUG` only when needed
- Keep REST/WS logs separated with `logback-spring.xml` channel-based sifting appenders (`/var/log/sigkill/REST.log`, `/var/log/sigkill/WS.log`).

## Testing Guidelines
- JUnit 5 + Spring Boot Test + AssertJ
- `@Nested`, 명시적 Given/When/Then 주석
- 테스트명은 서술형(이 저장소는 underscore + 한국어 스타일 사용)
- 핵심 비즈니스 로직의 정상/예외/경계 케이스 모두 커버

## Room Domain Rules
### 방 생성
- 4자리 랜덤 방 번호(1000~9999), 중복 체크
- 방 제목: 2~20자, 한글/영문/공백 허용
- 정원: `RoomConstants` 범위 검증
- 방장 권한의 단일 소스는 `Room.hostId`

### 방 조회/입장
- 목록 조회는 `WAITING` 상태만
- `INGAME` 상태 방은 입장 불가
- 정원 초과 시 입장 불가

## Game Domain Working Memory
- 게임 시작 시 `Game`, `GamePlayer` 생성
- `GamePlayer`: `score=0`, `status=ALIVE`로 시작
- `Game`은 문제 순서 `quizIds`(불변 리스트) 보유. 생성 시 `List.copyOf(...)`로 고정
- 라운드 시작 시 `currentQuizIndex` 증가, `roundStartedAtMillis = Instant.now().toEpochMilli()` 기록
- 라운드 제한시간은 `Game.ROUND_COUNTDOWN_MILLIS = 5_000L`
- 제출은 제한시간 내 요청만 처리, 유저별 마지막 제출을 최종 제출로 사용
- 라운드 종료 시 정답자 `+1점`, 오답/미제출자는 `DEAD`
- 종료 조건: 전원 사망 / 1명 생존 / 모든 문제 소진
- 게임 종료 후 결과(score 기반) 노출 후 `Game`, `GamePlayer`, `SelectedChoice` 삭제
- 종료 후 또는 마감 후 지연 제출은 `status`, `quizId`, `deadline` 검증에서 무시

## Data Model (In-Memory / Redis-Style Key Design)
### User
- Key: `user:{userId}`
- Fields:
  - `userId` (BIGINT, Key)
  - `sessionId` (STRING)
  - `nickname` (STRING)
  - `role` (GUEST / USER / ADMIN)
- TTL: 세션 만료 시 자동 삭제

### Room
- Key: `room:{roomId}`
- Fields:
  - `roomId` (STRING, Key)
  - `roomTitle` (STRING)
  - `capacity` (INT)
  - `hostId` (BIGINT)
  - `status` (WAITING / INGAME)
- TTL: 방 폭파 시 명시적 삭제

### Player
- Key: `player:{userId}`
- Fields:
  - `userId` (BIGINT, Key)
  - `roomId` (STRING)
  - `nickname` (STRING)
  - `readyStatus` (NOT_READY / READY)
- TTL: 방 폭파 시 명시적 삭제

### GamePlayer
- Key: `gamePlayer:{gameId}:{userId}`
- Fields:
  - `userId` (BIGINT, Key)
  - `gameId` (BIGINT)
  - `score` (BIGINT)
  - `gamePlayerStatus` (ALIVE / DEAD)
- TTL: 게임 종료 후 명시적 삭제

### Game
- Key: `game:{roomId}`
- Fields:
  - `gameId` (BIGINT)
  - `roomId` (STRING)
  - `quizIds` (List<BIGINT>)
  - `currentQuizIndex` (INT)
  - `roundStartedAtMillis` (LONG, epoch ms)
- TTL: 게임 종료 후 명시적 삭제(필요 시 결과 조회 유예)

### SelectedChoice
- Key: `selected:{gameId}:{userId}`
- Fields:
  - `quizId` (BIGINT)
  - `choiceId` (BIGINT)
- TTL: 게임 종료 후 명시적 삭제

### Static Quiz Data
DB 저장 없이 JSON/하드코딩으로 관리하며 서버 시작 시 1회 로드 후 불변 참조.

## STOMP Documentation Rule
- STOMP 경로(`@MessageMapping`), 토픽/큐 목적지, 이벤트 type/필드, 에러 코드 계약 변경 시 같은 PR에서 `docs/STOMP_MESSAGE_SPEC.md`를 함께 수정
- STOMP 문서는 단일 파일로 유지. 중복 가이드 문서 추가 금지

## Known Issues / TODO
- `RoomService.fetchRooms()`의 `return null` 버그 확인 필요
- 플레이어의 다중 방 접속 처리 로직 보강 필요
- 방 목록 정렬 로직 개선 필요

## Commit & Pull Request Guidelines
- 커밋 포맷: `feat|fix|refactor|test|docs|chore: #<issue> <summary>`
- 예시: `refactor: #8 RoomService 메서드의 sessionId를 userId로 변경`
- PR 필수 포함:
  - 변경 요약 및 이유
  - 연동 이슈(`#8` 등)
  - 테스트 증거(`./gradlew test` 핵심 결과)
  - API/WebSocket 요청-응답 예시(동작 변경 시)
