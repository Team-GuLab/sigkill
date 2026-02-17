# sigkill-server

실시간 방(룸) 기반 게임 로비를 위한 Spring Boot 서버입니다.

## 빠른 시작

### 요구 사항

- Java 21
- Gradle Wrapper 사용 (`./gradlew`)

### 실행

```bash
./gradlew bootRun
```

### 테스트

```bash
./gradlew test
```

## 주요 링크

- WebSocket 테스트 페이지: [http://localhost:8080/room-ws-test.html](http://localhost:8080/room-ws-test.html)
- Swagger UI: [http://localhost:8080/swagger-ui/index.html#/](http://localhost:8080/swagger-ui/index.html#/)
- STOMP 계약 문서: [docs/STOMP_MESSAGE_SPEC.md](docs/STOMP_MESSAGE_SPEC.md)

## REST 기능

### 인증

- `POST /api/v1/users/guest-login`
  - 세션 기반 게스트 로그인

### 방 관리

- `GET /api/v1/rooms`
  - 방 목록 조회 (페이징)
- `POST /api/v1/rooms`
  - 방 생성
- `GET /api/v1/rooms/{roomId}/availability`
  - 방 입장 가능 여부 확인

## STOMP 기능

### 연결 규칙

- Endpoint: `/ws`
- App Prefix: `/app`
- Broker Prefix: `/topic`, `/queue`

### Room 명령

- `SEND /app/room/join`
- `SEND /app/room/leave`
- `SEND /app/room/ready`
- `SEND /app/room/unready`

### 구독 채널

- Room 브로드캐스트: `SUBSCRIBE /topic/room/{roomId}`
- 사용자 에러: `SUBSCRIBE /user/queue/errors`
- 사용자 pong: `SUBSCRIBE /user/queue/pong`
- 사용자 room snapshot: `SUBSCRIBE /user/queue/room/snapshot`

### 이벤트 타입

- `PLAYER_JOIN`
- `PLAYER_LEFT`
- `HOST_CHANGED`
- `PLAYER_READY`
- `PLAYER_UNREADY`
- `PONG`

## 최근 추가된 실시간 안정성 기능

### 1) STOMP Heartbeat

- `SimpleBroker` heartbeat 활성화
- 값: `10000ms / 10000ms` (server->client / client->server)

### 2) Ping / Pong

- `SEND /app/ping` -> `SUBSCRIBE /user/queue/pong`
- 응답 필드: `type`, `userId`, `serverTime(UTC ISO-8601)`

### 3) 비정상 종료 자동 퇴장 처리

- 클라이언트가 `leave` 없이 연결이 끊겨도 서버가 자동으로 퇴장 처리
- 필요 시 `PLAYER_LEFT`, `HOST_CHANGED`를 기존 room topic으로 브로드캐스트

### 4) SUBSCRIBE 인가 강화

- `/topic/room/{roomId}`는 방 멤버만 구독 가능
- 사용자 큐는 허용 목록만 구독 가능:
  - `/user/queue/errors`
  - `/user/queue/pong`
  - `/user/queue/room/snapshot`

### 5) Join 시 스냅샷 개인 전송

- `join` 성공 시 요청자에게 동일 payload를 `/user/queue/room/snapshot`으로 추가 전송
- 그 외 참여자에게는 기존처럼 `/topic/room/{roomId}`로 브로드캐스트

## WebSocket 테스트 페이지 사용 흐름

1. 게스트 로그인 실행
2. 방 목록 조회 또는 테스트 데이터 생성
3. 방 참가 통합 실행
   - availability 확인
   - `/user/queue/errors`, `/user/queue/room/snapshot` 구독
   - `SEND /app/room/join`
   - `SUBSCRIBE /topic/room/{roomId}`
4. `READY/UNREADY`, `LEAVE + DISCONNECT` 테스트
5. `PING 전송`으로 pong 응답 확인

정확한 payload, 에러 코드, 이벤트 계약은 `docs/STOMP_MESSAGE_SPEC.md`를 기준으로 확인하세요.
