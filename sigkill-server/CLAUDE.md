# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

실시간 퀴즈 게임 플랫폼 - 여러 사용자가 방에 참여하여 웹소켓 기반으로 실시간 퀴즈를 풀고 경쟁하는 게임 서버

- **기술 스택**: Spring Boot 3.5.10, Java 21, Spring WebSocket, Spring Security
- **인증**: 세션 기반 비회원(게스트) 인증
- **실시간 통신**: WebSocket STOMP 프로토콜
- **데이터 저장**: Java 메모리 (방 정보, 사용자 정보 in-memory 저장)

## 프로젝트 명령어

### 빌드 및 실행
```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test

# 단일 테스트 실행
./gradlew test --tests "ClassName.methodName"

# clean & build
./gradlew clean build
```

### Swagger UI
- 서버 실행 후 `/swagger-ui/index.html` 접속

## 아키텍처 구조

### 도메인 계층 구조
```
com.gulab.sigkillserver.domain
├── user/          # 사용자 도메인 (비회원 로그인)
│   ├── model/     # User 엔티티, Role
│   ├── repository/# UserRepository
│   ├── service/   # UserService (비회원 로그인 처리)
│   ├── controller/# UserController (POST /api/v1/users/guest-login)
│   └── util/      # NicknameGenerator
└── room/          # 방 도메인
    ├── model/     # Room 엔티티, RoomStatus (WAITING, INGAME)
    ├── repository/# RoomRepository
    ├── service/   # RoomService (방 생성, 조회, 입장 가능 여부)
    ├── controller/# RoomController (REST API)
    └── constant/  # RoomConstants (방 인원, 제목 제약)
```

### 인증 및 보안
- **Spring Security + Session**: 비회원 로그인 시 세션 ID를 사용자 식별자로 사용
- **SecurityConfig** (src/main/java/com/gulab/sigkillserver/config/security/SecurityConfig.java:22):
  - CSRF 비활성화, CORS 전체 허용
  - `/api/**` 경로는 `ROLE_GUEST` 권한 필요
  - `/api/v1/users/guest-login`, `/ws/**`, Swagger는 인증 없이 접근 가능
  - 세션 기반 인증: `HttpSessionSecurityContextRepository` 사용
  - 세션 타임아웃: 24시간 (application.properties)

### 세션 및 인증 흐름
1. 클라이언트가 `POST /api/v1/users/guest-login` 호출
2. 세션 ID 기반으로 신규/기존 사용자 조회 (UserMemoryRepository)
3. 신규 사용자 시 랜덤 닉네임 생성 (`NicknameGenerator`)
4. `UsernamePasswordAuthenticationToken` 생성 후 `SecurityContext`에 저장
5. 세션에 `SPRING_SECURITY_CONTEXT` 저장
6. 이후 모든 API 요청 시 `Principal`에서 세션 ID 조회 가능

### 인메모리 데이터 관리
- **RoomRepository**: Room 엔티티를 Java `ConcurrentHashMap`으로 인메모리 저장
- **UserRepository**: User 엔티티를 Java `ConcurrentHashMap`으로 인메모리 저장
- 서버 재시작 시 모든 데이터가 초기화됨 (휘발성)
- Thread-safe를 위해 `ConcurrentHashMap` 사용

### 공통 컴포넌트
- **BaseResponse** (src/main/java/com/gulab/sigkillserver/common/BaseResponse.java:8): 모든 API 응답을 `BaseResponse<T>` 형식으로 래핑
  - `timeStamp`, `code`, `message`, `result` 필드 포함
  - `ZonedDateTime` 사용 (ISO 8601 형식)
- **CustomException + GlobalExceptionHandler**: 도메인별 `CustomErrorCode` 정의, 전역 예외 처리
- **RoomErrorCode**: `ROOM_NOT_FOUND`, `ROOM_FULL`, `ROOM_IN_GAME`, `ROOM_CREATE_ERROR` 등

### WebSocket 구조 (진행 중)
- **STOMP 프로토콜** 사용 예정
- **RoomWebSocketController** (src/main/java/com/gulab/sigkillserver/domain/room/controller/RoomWebSocketController.java): WebSocket 메시지 처리
- `/ws/**` 엔드포인트 사용

## 주요 비즈니스 로직

### 방 생성 (RoomService.createRoom)
- 4자리 랜덤 방 번호 생성 (1000~9999), 중복 체크
- 방 제목: 2~20자, 한글/영문/공백만 허용
- 방 인원: MIN_CAPACITY ~ MAX_CAPACITY (RoomConstants)
- 호스트는 방 생성자 세션 ID로 지정
- 방장 권한의 단일 소스는 `Room.hostId`이며, `Player` 엔티티에는 role을 두지 않음

### 방 목록 조회 (RoomService.fetchRooms)
- `WAITING` 상태 방만 조회, 페이지네이션 적용
- 현재 구현에 `return null` 버그 있음 (src/main/java/com/gulab/sigkillserver/domain/room/service/RoomService.java:64)

### 방 입장 가능 여부 (RoomService.checkRoomAvailability)
- `INGAME` 상태면 입장 불가
- 정원 초과 시 입장 불가
- WebSocket 연결 정보 반환 (`WebSocketInfo`)

## 게임 규칙 (명세 기반)

자세한 명세는 `docs/spec.md` 참조.

### 퀴즈 규칙
- 오답/시간 초과 → 즉시 사망
- 생존자만 다음 문제 진행
- 마지막 1명 생존 또는 퀴즈 10개 종료 시 게임 종료
- 한 판 종료 시 모두 부활

### 정답 판정
- 제한 시간: 5초 (서버 기준 시간)
- 허용 오차: 500ms (네트워크 지연 고려)
- 좌표가 아닌 **선택지 기반**으로 정답 판정

### 시간 형식
- **ISO 8601** Date and time with offset 사용 (`2026-01-29T14:23:45.123+09:00`)
- Java에서는 `ZonedDateTime` 사용

## 개발 시 주의사항

### REST API 경로
- `RoomController.getRooms()` (src/main/java/com/gulab/sigkillserver/domain/room/controller/RoomController.java:28): `/api/v1/rooms/v1/rooms` 중복 경로 (수정 필요)

### 미완성 TODO
- `RoomService.fetchRooms()` return null 버그 수정 필요
- 플레이어가 이미 다른 방에 접속한 경우 처리 로직 필요
- 방 목록 정렬 로직 개선 필요

### Principal 사용법
- Controller에서 `Principal principal` 주입 시 `principal.getName()`으로 세션 ID 조회

## Redis 데이터 구조

### 동적 데이터

#### User
**Key:** `user:{sessionId}`

| 필드명 | 설명 | 타입 |
| --- | --- | --- |
| userId | 유저 아이디 | BIGINT |
| nickname | 닉네임 | STRING |
| sessionId | 세션 아이디 | STRING |

**TTL:** 세션 만료 시 자동 삭제

#### Room
**Key:** `room:{roomId}`

| 필드명 | 설명 | 타입 |
| --- | --- | --- |
| roomId | 방 아이디 | STRING |
| roomTitle | 방 제목 | STRING |
| hostId | 방장 유저 아이디 | BIGINT |
| capacity | 수용 가능 인원 | INT |
| status | 방 상태 | WAITING / INGAME |

**TTL:** 방 폭파 시 명시적 삭제

#### Player
**Key:** `player:{roomId}:{userId}`

| 필드명 | 설명 | 타입 |
| --- | --- | --- |
| playerId | 플레이어 아이디 | BIGINT |
| userId | 유저 아이디 | BIGINT |
| roomId | 방 아이디 | STRING |
| readyStatus | 준비 상태 | WAITING / READY |
| gameStatus | 게임 상태 | ALIVE / DEAD / NOT_GAME |
| score | 점수 | BIGINT |

**TTL:** 방 폭파 시 명시적 삭제

#### Game
**Key:** `game:{roomId}`

| 필드명 | 설명 | 타입 |
| --- | --- | --- |
| gameId | 게임 아이디 | BIGINT |
| roomId | 방 아이디 | STRING |
| status | 게임 상태 | IN_PROGRESS / FINISHED |
| currentQuizOrder | 현재 퀴즈 순서 | INT |
| quizStartTime | 퀴즈 시작 시간 | LONG (epoch ms) |
| quizIds | 출제 퀴즈 목록 | List\<BIGINT\> |

**TTL:** 게임 종료 후 명시적 삭제 (결과 조회 필요 시 유예 시간 부여)

#### SelectedChoice
**Key:** `selected:{gameId}:{userId}`

| 필드명 | 설명 | 타입 |
| --- | --- | --- |
| quizId | 퀴즈 아이디 | BIGINT |
| choiceId | 선택한 선지 아이디 | BIGINT |

**TTL:** 게임 종료 후 명시적 삭제

### 정적 데이터

퀴즈 데이터는 DB 불필요. JSON 파일 또는 하드코딩으로 관리

```json
[
  {
    "quizId": 1,
    "question": "문제 내용",
    "explanation": "해설 내용",
    "correctChoiceId": 2,
    "choices": [
      { "choiceId": 1, "text": "선지 1" },
      { "choiceId": 2, "text": "선지 2" },
      { "choiceId": 3, "text": "선지 3" },
      { "choiceId": 4, "text": "선지 4" }
    ]
  }
]
```
