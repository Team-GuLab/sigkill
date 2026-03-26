# STOMP Message Spec

이 문서는 현재 서버 구현 기준의 STOMP 계약(Single Source of Truth)이다.

## 1. 범위

- 현재 구현/계약 범위: Room 도메인 이벤트 (`join`, `snapshot`, `bot`, `leave`, `ready`, `unready`), Game 도메인 이벤트 (`game/start`,
  `quiz/start`, `submit`, `quiz/end`, `game/end`), 연결 상태 확인 이벤트 (`ping`)
- 마이그레이션 노트:
    - REST `GET /api/v1/rooms/{roomId}/availability` 삭제
    - 방 입장은 REST `POST /api/v1/rooms/{roomId}/join` 후 STOMP `SEND /app/room/join`으로 최종 확정된다

## 2. 연결 및 목적지 규칙

- WebSocket Endpoint: `/ws` (SockJS 미사용)
- Application Prefix: `/app`
- Broker Prefix: `/topic`, `/queue`
- Room Broadcast 채널: `/topic/room/{roomId}`
- Game Broadcast 채널: `/topic/game/{gameId}`
- 사용자 에러 채널: `/user/queue/errors`
- 사용자 pong 채널: `/user/queue/pong`
- Heartbeat: `10000ms / 10000ms` (server->client / client->server)

## 3. 인증/인가

- 인증은 세션 기반이며, STOMP 프레임은 `StompHandler`에서 `accessor.getUser()`로 검증한다.
- 인증 실패 시 에러 코드 `ACCESS_DENIED`를 사용자 에러 채널로 전송한다.
- Request payload에는 사용자 식별자(`userId`, `sessionId`)를 넣지 않는다. 서버가 `Principal`에서 추출한다.
- 구독 인가 규칙:
    - `/topic/room/{roomId}`는 다음 조건에서 구독 가능
        - 현재 해당 방 멤버인 사용자
        - 위 조건을 만족하지 않으면 구독 불가
    - `/topic/game/{gameId}`는 다음 조건에서 구독 가능
        - 해당 `gameId`가 속한 방의 현재 멤버인 사용자
        - 위 조건을 만족하지 않으면 구독 불가
    - 사용자 큐 구독 허용 대상: `/user/queue/errors`, `/user/queue/pong`

## 4. 공통 DTO

### 4.1 RoomIdCommand Request (leave/ready/unready/start)

```json
{
  "roomId": "1234"
}
```

- 타입: `RoomIdCommand`
- 제약: `roomId`는 공백 불가(`@NotBlank`)

### 4.2 RoomIdCommand Request (join/snapshot)

```json
{
  "roomId": "1234"
}
```

- 타입: `RoomIdCommand`
- 제약: `roomId`는 공백 불가(`@NotBlank`)
- 사용 경로: `SEND /app/room/join`, `SEND /app/room/snapshot`
- 선행 REST 의미:
    - `POST /api/v1/rooms/{roomId}/join`은 같은 사용자/같은 방 재호출 시 현재 방 정보를 그대로 성공 재응답한다
    - 같은 방 재호출은 기존 `PENDING` timeout을 연장하지 않으며 중복 `Player`를 만들지 않는다
    - 다른 방에 이미 참가 중인 사용자의 호출만 `409 USER_ALREADY_IN_ROOM`이다
- 클라이언트 순서:
    1. REST `POST /api/v1/rooms/{roomId}/join` 또는 `POST /api/v1/rooms`
    2. `SUBSCRIBE /topic/room/{roomId}`
    3. 가능하면 즉시 `SEND /app/room/join`
    4. `SEND /app/room/snapshot`, `SEND /app/room/ready` 등 나머지 명령 전송

### 4.3 Shared Response

```json
{
  "room": {
    "roomId": "1234",
    "roomTitle": "재미있는 퀴즈방",
    "hostId": 1,
    "capacity": 6,
    "status": "WAITING"
  },
  "player": {
    "userId": 2,
    "nickname": "귀여운사자",
    "status": "NOT_READY",
    "role": "GUEST"
  }
}
```

- `RoomInfo.status`: `WAITING | INGAME`
- `PlayerInfo.status`: `READY | NOT_READY`
- `PlayerInfo.role`: `HOST | GUEST`

### 4.4 Game Request DTO

`GAME_START` 요청

```json
{
  "roomId": "1234"
}
```

`CHOICE_SUBMIT` 요청

```json
{
  "gameId": 77,
  "quizId": 1001,
  "choiceNumber": 3
}
```

### 4.5 Game Response Envelope (MVP)

Game 이벤트 응답은 공통 Envelope를 사용한다.

```json
{
  "type": "EVENT_TYPE",
  "roomId": "1234",
  "gameId": 77,
  "occurredAt": 1771417642829,
  "payload": {}
}
```

## 5. Room 이벤트 계약

### 5.1 플레이어 입장 알림

- SEND: `/app/room/join`
- SUBSCRIBE: `/topic/room/{roomId}`
- Response type: `PLAYER_JOIN`
- 요청 payload는 `RoomIdCommand`
- 의미: REST로 생성/입장된 `PENDING` 플레이어를 최종 `ACTIVE` 상태로 확정한다
- 선행 조건:
    - REST `POST /api/v1/rooms/{roomId}/join` 또는 `POST /api/v1/rooms`가 먼저 성공해야 함
    - `/topic/room/{roomId}` 구독 직후 가장 먼저 전송해야 함
- 서버 동작:
    - 같은 사용자의 같은 방 REST 재호출은 성공으로 재응답하지만 `PENDING` timeout은 유지한다
    - 첫 성공 호출에만 `PLAYER_JOIN`을 브로드캐스트한다
    - 이미 `ACTIVE`인 사용자의 재호출은 no-op 이다
    - REST 후 10초 안에 이 요청이 오지 않으면 서버가 `PENDING` 플레이어를 자동 정리한다

```json
{
  "roomId": "1234"
}
```

응답 예시:

```json
{
  "type": "PLAYER_JOIN",
  "room": {
    "roomId": "1234",
    "roomTitle": "재미있는 퀴즈방",
    "hostId": 1,
    "capacity": 6,
    "status": "WAITING"
  },
  "player": {
    "userId": 2,
    "nickname": "참가자",
    "status": "NOT_READY",
    "role": "GUEST"
  }
}
```

### 5.1.1 방 스냅샷 조회

- SEND: `/app/room/snapshot`
- SUBSCRIBE: `/topic/room/{roomId}`
- Response type: `ROOM_SNAPSHOT`
- 요청 payload는 `RoomIdCommand`
- 선행 조건: REST `POST /api/v1/rooms/{roomId}/join` 또는 `POST /api/v1/rooms`가 먼저 성공해야 함
- `SEND /app/room/join` 전의 `PENDING` 상태에서도 호출할 수 있다
- 응답의 `players` 목록에는 현재 방 멤버 전체가 포함되며, `PENDING` 플레이어도 포함될 수 있다

응답 예시:

```json
{
  "type": "ROOM_SNAPSHOT",
  "room": {
    "roomId": "1234",
    "roomTitle": "재미있는 퀴즈방",
    "hostId": 1,
    "capacity": 6,
    "status": "WAITING"
  },
  "players": [
    {
      "userId": 1,
      "nickname": "방장",
      "status": "NOT_READY",
      "role": "HOST"
    },
    {
      "userId": 2,
      "nickname": "참가자",
      "status": "NOT_READY",
      "role": "GUEST"
    }
  ]
}
```

### 5.1.2 봇 추가

- SEND: `/app/room/bot`
- SUBSCRIBE: `/topic/room/{roomId}`
- 요청 payload는 `RoomIdCommand`
- 권한/조건:
    - 방장만 호출 가능
    - `WAITING` 상태 방에서만 호출 가능
    - 정원이 가득 찬 방에서는 실패
- 서버 동작:
    - 서버는 `UserRole.BOT` 사용자를 내부 생성하고 닉네임은 항상 `[봇] ` 접두사를 사용한다
    - 서버는 기존 서비스 경로로 `joinRoom -> confirmJoin`을 호출해 `PLAYER_JOIN`을 먼저 브로드캐스트한다
    - 이어서 짧은 랜덤 지연 후 기존 서비스 경로로 `readyPlayer`를 호출해 `PLAYER_READY`를 브로드캐스트한다
    - 따라서 `/app/room/bot` 성공은 단일 이벤트가 아니라 `PLAYER_JOIN -> PLAYER_READY` 순서의 연속 이벤트로 관찰된다

### 5.2 플레이어 퇴장

- SEND: `/app/room/leave`
- SUBSCRIBE: `/topic/room/{roomId}`
- Response type: `PLAYER_LEFT`

```json
{
  "type": "PLAYER_LEFT",
  "player": {
    "userId": 2,
    "nickname": "참가자",
    "status": "READY",
    "role": "GUEST"
  }
}
```

추가 규칙:

- 퇴장한 사용자가 방장이면 같은 채널로 `HOST_CHANGED`를 추가 전송한다.
- 마지막 1명이 퇴장하면 방이 삭제되며 `HOST_CHANGED`는 전송되지 않는다.
- 클라이언트가 명시적으로 `leave`를 보내지 않고 연결이 끊겨도 서버는 자동 퇴장 처리 후 동일 이벤트를 브로드캐스트한다.
- `leave` 또는 연결 종료 후 서버는 game topic으로 synthetic `GAME_LOADED`를 만들지 않는다.

### 5.3 방장 변경(자동 이벤트)

- SUBSCRIBE: `/topic/room/{roomId}`
- Response type: `HOST_CHANGED`

```json
{
  "type": "HOST_CHANGED",
  "newHost": {
    "userId": 3,
    "nickname": "새 방장",
    "status": "NOT_READY",
    "role": "HOST"
  },
  "oldHost": {
    "userId": 1,
    "nickname": "이전 방장",
    "status": "NOT_READY",
    "role": "GUEST"
  },
  "reason": "HOST_LEFT"
}
```

### 5.4 플레이어 준비

- SEND: `/app/room/ready`
- SUBSCRIBE: `/topic/room/{roomId}`
- Response type: `PLAYER_READY`

```json
{
  "type": "PLAYER_READY",
  "player": {
    "userId": 2,
    "nickname": "참가자",
    "status": "READY",
    "role": "GUEST"
  },
  "allReady": false
}
```

`allReady` 규칙:

- 호스트를 제외한 모든 플레이어가 `READY`면 `true`
- 봇 추가 성공 시에는 `PLAYER_JOIN` 뒤 지연된 `PLAYER_READY`가 추가로 전송될 수 있다

### 5.5 플레이어 준비 취소

- SEND: `/app/room/unready`
- SUBSCRIBE: `/topic/room/{roomId}`
- Response type: `PLAYER_UNREADY`

```json
{
  "type": "PLAYER_UNREADY",
  "player": {
    "userId": 2,
    "nickname": "참가자",
    "status": "NOT_READY",
    "role": "GUEST"
  }
}
```

### 5.6 Ping / Pong (연결 상태 확인)

- SEND: `/app/ping`
- SUBSCRIBE: `/user/queue/pong`
- Response type: `PONG`

```json
{
  "type": "PONG",
  "userId": "2",
  "serverTime": 1739791230456
}
```

설명:

- `userId`는 서버가 `Principal`에서 추출한다.
- `serverTime`은 서버 시각의 Unix epoch milliseconds(`long`)이다.

## 6. Game 이벤트 계약 (MVP)

### 6.1 게임 시작

- SEND: `/app/room/start`
- SUBSCRIBE: `/topic/room/{roomId}`
- Response type: `GAME_START`
- 권한/조건:
    - 방장만 시작 가능
    - 게임 시작 시 호스트를 제외한 모든 플레이어가 `READY`여야 함

요청 예시:

```json
{
  "roomId": "1234"
}
```

응답 예시:

```json
{
  "type": "GAME_START",
  "roomId": "1234",
  "gameId": 77,
  "occurredAt": 1771417642829,
  "payload": {
    "quiz": {
      "currentQuizIndex": 0,
      "totalQuizCount": 10
    },
    "players": [
      {
        "userId": 1,
        "nickname": "호스트유저",
        "status": "ALIVE",
        "quizResult": "NONE",
        "score": 0
      },
      {
        "userId": 2,
        "nickname": "게스트유저",
        "status": "ALIVE",
        "quizResult": "NONE",
        "score": 0
      }
    ]
  }
}
```

초기값 규칙:

- `GAME_START.payload.players[*]`는 `QuizEndPlayerInfo` 스키마를 사용한다.
- 게임 시작 시점에는 모든 플레이어가 `status=ALIVE`, `quizResult=NONE`, `score=0`으로 내려간다.

### 6.2 게임 로딩 완료 동기화

- SEND: `/app/game/load`
- SUBSCRIBE: `/topic/game/{gameId}`
- Response type: `GAME_LOADED`

요청 예시:

```json
{
  "gameId": 77
}
```

응답 예시:

```json
{
  "type": "GAME_LOADED",
  "roomId": "1234",
  "gameId": 77,
  "occurredAt": 1771417642829,
  "payload": {
    "players": [
      {
        "userId": 1,
        "nickname": "원일",
        "isLoaded": true
      },
      {
        "userId": 15432,
        "nickname": "선호",
        "isLoaded": false
      },
      {
        "userId": 53,
        "nickname": "성재",
        "isLoaded": true
      },
      {
        "userId": 12,
        "nickname": "상현",
        "isLoaded": false
      }
    ],
    "allLoaded": false
  }
}
```

동작 규칙:

- 클라이언트는 `GAME_START` 수신 후 게임 화면 진입/초기화가 완료되면 `/app/game/load`를 전송한다.
- 서버는 `GAME_LOADED`를 `/topic/game/{gameId}`로 브로드캐스트한다.
- 봇도 `GAME_START` 이후 서버 내부에서 실제 `loadGame()`을 호출하며, 그 결과만 `GAME_LOADED`로 브로드캐스트된다.
- `players[*].isLoaded`는 현재 방에 남아 있는 각 사용자별 로딩 완료 여부를 의미한다.
- `payload.allLoaded=true`는 현재 방에 남아 있는 게임 참가자 전원의 `isLoaded=true`일 때만 성립한다.
- `allReady`(방 준비 상태)와 `allLoaded`(게임 로딩 상태)는 다른 값이다.
- `payload.allLoaded=true`가 되는 시점에 서버는 최초 1회만 3초 뒤 첫 `QUIZ_START`를 자동 브로드캐스트한다.

### 6.3 퀴즈 시작

- SUBSCRIBE: `/topic/game/{gameId}`
- Response type: `QUIZ_START`

응답 예시:

```json
{
  "type": "QUIZ_START",
  "roomId": "1234",
  "gameId": 77,
  "occurredAt": 1771417642829,
  "payload": {
    "quiz": {
      "quizId": 1001,
      "currentQuizIndex": 1,
      "totalQuizCount": 10,
      "startTime": 1771417642829,
      "endTime": 1771417647829,
      "question": "리액트의 핵심 개념이 아닌것은?",
      "choices": [
        {
          "number": 1,
          "text": "Component"
        },
        {
          "number": 2,
          "text": "Virtual DOM"
        },
        {
          "number": 3,
          "text": "JSX"
        },
        {
          "number": 4,
          "text": "SQL Query"
        }
      ]
    }
  }
}
```

동작 규칙:

- `GAME_LOADED` 응답에서 `payload.allLoaded=true`가 된 이후 서버는 3초 대기 후 `QUIZ_START`를 자동 브로드캐스트한다.
- 살아 있는 봇은 `QUIZ_START` 이후 서버 내부에서 지연된 `submitChoice()`를 호출하고, 결과는 기존 `CHOICE_SUBMIT`으로만 노출된다.
- `QUIZ_START` 이후 서버는 10초 대기 후 `QUIZ_END`를 자동 브로드캐스트한다.
- `QUIZ_END` 이후 게임 종료 조건이면 같은 채널에 `GAME_END`를 브로드캐스트하고 종료한다.
- 게임 미종료면 `QUIZ_END` 이후 10초 대기 후 다음 `QUIZ_START`를 자동 브로드캐스트한다.

### 6.4 답 제출

- SEND: `/app/game/submit`
- SUBSCRIBE: `/topic/game/{gameId}`
- Response type: `CHOICE_SUBMIT`

요청 예시:

```json
{
  "gameId": 77,
  "quizId": 1001,
  "choiceNumber": 3
}
```

추가 규칙:

- `GAME_END` 후 방에 사람이 1명 이상 남아 있으면 봇은 room topic으로 정상 `PLAYER_READY`를 다시 브로드캐스트한다.
- `GAME_END` 후 사람이 0명이면 봇은 기존 `leaveRoom()` 경로로 순차 퇴장하며 room 정리를 완료한다.

응답 예시:

```json
{
  "type": "CHOICE_SUBMIT",
  "roomId": "1234",
  "gameId": 77,
  "occurredAt": 1771417645000,
  "payload": {
    "quiz": {
      "quizId": 1001,
      "currentQuizIndex": 1,
      "totalQuizCount": 10
    },
    "actor": {
      "userId": 2,
      "nickname": "차분한 모닥이"
    },
    "choiceNumber": 1
  }
}
```

### 6.5 퀴즈 종료

- SUBSCRIBE: `/topic/game/{gameId}`
- Response type: `QUIZ_END`

응답 예시:

```json
{
  "type": "QUIZ_END",
  "roomId": "1234",
  "gameId": 77,
  "occurredAt": 1771417647829,
  "payload": {
    "quiz": {
      "quizId": 1001,
      "currentQuizIndex": 1,
      "totalQuizCount": 10
    },
    "answer": {
      "correctChoiceNumber": 4,
      "explanation": "SQL Query는 데이터베이스의 개념입니다."
    },
    "players": [
      {
        "userId": 1,
        "nickname": "원일",
        "status": "ALIVE",
        "quizResult": "CORRECT",
        "score": 3
      },
      {
        "userId": 15432,
        "nickname": "선호",
        "status": "DEAD",
        "quizResult": "WRONG",
        "score": 1
      },
      {
        "userId": 53,
        "nickname": "성재",
        "status": "DEAD",
        "quizResult": "NO_SUBMISSION",
        "score": 0
      },
      {
        "userId": 12,
        "nickname": "상현",
        "status": "DEAD",
        "quizResult": "SKIPPED_DEAD",
        "score": 2
      }
    ]
  }
}
```

### 6.6 게임 종료

- SUBSCRIBE: `/topic/game/{gameId}`
- Response type: `GAME_END`

응답 예시:

```json
{
  "type": "GAME_END",
  "roomId": "1234",
  "gameId": 77,
  "occurredAt": 1771417655000,
  "payload": {
    "reason": "ONE_SURVIVOR",
    "rankings": [
      {
        "rank": 1,
        "userId": 1,
        "nickname": "원일",
        "score": 5
      },
      {
        "rank": 2,
        "userId": 12,
        "nickname": "상현",
        "score": 2
      },
      {
        "rank": 3,
        "userId": 15432,
        "nickname": "선호",
        "score": 1
      },
      {
        "rank": 4,
        "userId": 53,
        "nickname": "성재",
        "score": 0
      }
    ]
  }
}
```

### 6.7 Game Enum

`PlayerStatus`

- `ALIVE`
- `DEAD`

`QuizResult`

- `CORRECT`
- `WRONG`
- `NO_SUBMISSION`
- `SKIPPED_DEAD`

`GameEndReason`

- `ALL_DEAD`: 전원 탈락으로 종료
- `ONE_SURVIVOR`: 생존자 1명 남아 종료
- `QUIZ_EXHAUSTED`: 마지막 문제까지 모두 진행해서 종료

## 7. 에러 계약

에러는 모두 사용자 채널(`/user/queue/errors`)로 내려간다.
Response type: `ERROR`

```json
{
  "type": "ERROR",
  "code": "ROOM_FULL",
  "message": "방이 가득 찼습니다"
}
```

주요 코드:

- 비즈니스: `ROOM_NOT_FOUND`, `ROOM_FULL`, `ROOM_IN_GAME`, `HOST_CANNOT_READY`, `PLAYER_NOT_IN_ANY_ROOM`,
  `PLAYER_NOT_IN_ROOM`,
  `NOT_ENOUGH_PLAYERS_TO_START`,
  `PLAYERS_NOT_READY`,
  `USER_ALREADY_IN_ROOM`,
  `ROOM_NUMBER_ERROR`,
  `SUBMIT_CHOICE_NOT_CURRENT_QUIZ`,
  `SUBMIT_CHOICE_IS_AFTER_DEADLINE`
- 인증/보안: `ACCESS_DENIED`
- 요청 검증: `INVALID_REQUEST` (`@Valid @Payload` 검증 실패 포함)
- 상태/기타: `INVALID_STATE`, `INTERNAL_SERVER_ERROR`

## 8. 현재 서버 enum 값

```java
public enum RoomResponseType {
    PLAYER_JOIN,
    OTHER_PLAYER_JOIN, // 현재 미사용
    PLAYER_LEFT,
    HOST_CHANGED,
    PLAYER_READY,
    PLAYER_UNREADY
}
```

## 9. 문서 동기화 규칙

다음 항목이 변경되면 같은 PR에서 이 문서를 반드시 갱신한다.

- `@MessageMapping` 경로
- `convertAndSend` 목적지
- 이벤트 타입(enum) 및 payload 필드
- STOMP 예외 코드/메시지 계약
- heartbeat 주기 및 ping/pong 계약
