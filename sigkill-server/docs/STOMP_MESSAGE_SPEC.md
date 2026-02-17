# STOMP Message Spec

이 문서는 현재 서버 구현 기준의 STOMP 계약(Single Source of Truth)이다.

## 1. 범위

- 현재 구현 범위: Room 도메인 이벤트 (`join`, `leave`, `ready`, `unready`), 연결 상태 확인 이벤트 (`ping`)
- 미구현 범위: Game 도메인 실시간 이벤트 (계약 미확정)

## 2. 연결 및 목적지 규칙

- WebSocket Endpoint: `/ws` (SockJS 미사용)
- Application Prefix: `/app`
- Broker Prefix: `/topic`, `/queue`
- Room Broadcast 채널: `/topic/room/{roomId}`
- 사용자 에러 채널: `/user/queue/errors`
- 사용자 pong 채널: `/user/queue/pong`
- 사용자 room snapshot 채널: `/user/queue/room/snapshot`
- Heartbeat: `10000ms / 10000ms` (server->client / client->server)

## 3. 인증/인가

- 인증은 세션 기반이며, STOMP 프레임은 `StompHandler`에서 `accessor.getUser()`로 검증한다.
- 인증 실패 시 에러 코드 `ACCESS_DENIED`를 사용자 에러 채널로 전송한다.
- Request payload에는 사용자 식별자(`userId`, `sessionId`)를 넣지 않는다. 서버가 `Principal`에서 추출한다.
- 구독 인가 규칙:
  - `/topic/room/{roomId}`는 해당 방 멤버만 구독 가능
  - 사용자 큐 구독 허용 대상: `/user/queue/errors`, `/user/queue/pong`, `/user/queue/room/snapshot`

## 4. 공통 DTO

### 4.1 Request

```json
{
  "roomId": "1234"
}
```

- 타입: `RoomIdCommand`
- 제약: `roomId`는 공백 불가(`@NotBlank`)

### 4.2 Shared Response

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
    "status": "NOT_READY"
  }
}
```

- `RoomInfo.status`: `WAITING | INGAME`
- `PlayerInfo.status`: `READY | NOT_READY`

## 5. Room 이벤트 계약

### 5.1 플레이어 입장

- SEND: `/app/room/join`
- SUBSCRIBE: `/topic/room/{roomId}`
- Response type: `PLAYER_JOIN`

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
  "players": [
    {
      "userId": 1,
      "nickname": "방장",
      "status": "NOT_READY"
    },
    {
      "userId": 2,
      "nickname": "참가자",
      "status": "NOT_READY"
    }
  ]
}
```

설명:

- 현재 구현은 입장 시 변경분만이 아니라 방/플레이어 전체 스냅샷을 브로드캐스트한다.
- 방 참가 요청자는 동일 payload를 `/user/queue/room/snapshot`으로도 수신한다.

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
    "status": "READY"
  }
}
```

추가 규칙:

- 퇴장한 사용자가 방장이면 같은 채널로 `HOST_CHANGED`를 추가 전송한다.
- 마지막 1명이 퇴장하면 방이 삭제되며 `HOST_CHANGED`는 전송되지 않는다.
- 클라이언트가 명시적으로 `leave`를 보내지 않고 연결이 끊겨도 서버는 자동 퇴장 처리 후 동일 이벤트를 브로드캐스트한다.

### 5.3 방장 변경(자동 이벤트)

- SUBSCRIBE: `/topic/room/{roomId}`
- Response type: `HOST_CHANGED`

```json
{
  "type": "HOST_CHANGED",
  "newHost": {
    "userId": 3,
    "nickname": "새 방장",
    "status": "NOT_READY"
  },
  "oldHost": {
    "userId": 1,
    "nickname": "이전 방장",
    "status": "NOT_READY"
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
    "status": "READY"
  },
  "allReady": false
}
```

`allReady` 규칙:

- 호스트를 제외한 모든 플레이어가 `READY`면 `true`

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
    "status": "NOT_READY"
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
  "serverTime": "2026-02-17T10:20:30.456Z"
}
```

설명:

- `userId`는 서버가 `Principal`에서 추출한다.
- `serverTime`은 서버 UTC 시간(ISO-8601)이다.

## 6. 에러 계약

에러는 모두 사용자 채널(`/user/queue/errors`)로 내려간다.

```json
{
  "code": "ROOM_FULL",
  "message": "방이 가득 찼습니다"
}
```

주요 코드:

- 비즈니스: `ROOM_NOT_FOUND`, `ROOM_FULL`, `ROOM_IN_GAME`, `HOST_CANNOT_READY`, `PLAYER_NOT_IN_ANY_ROOM`, `PLAYER_NOT_IN_ROOM`,
  `USER_ALREADY_IN_ROOM`,
  `ROOM_NUMBER_ERROR`
- 인증/보안: `ACCESS_DENIED`
- 요청 검증: `INVALID_REQUEST` (`@Valid @Payload` 검증 실패 포함)
- 상태/기타: `INVALID_STATE`, `INTERNAL_SERVER_ERROR`

## 7. 현재 서버 enum 값

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

## 8. 문서 동기화 규칙

다음 항목이 변경되면 같은 PR에서 이 문서를 반드시 갱신한다.

- `@MessageMapping` 경로
- `convertAndSend` 목적지
- 이벤트 타입(enum) 및 payload 필드
- STOMP 예외 코드/메시지 계약
- heartbeat 주기 및 ping/pong 계약
