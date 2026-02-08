# STOMP 메시지 명세서

실시간 퀴즈 게임 플랫폼의 WebSocket STOMP 메시지 프로토콜 정의

## 목차

1. [개요](#개요)
2. [메시지 타입](#메시지-타입)
3. [방 (Room) 메시지](#방-room-메시지)
4. [게임 (Game) 메시지](#게임-game-메시지)
5. [채팅 (Chat) 메시지](#채팅-chat-메시지)

---

## 개요

### 엔드포인트 구조

- **연결**: `/ws` (SockJS)
- **Application Prefix**: `/app`
- **Broadcast**: `/topic/{destination}`
- **Personal**: `/queue/{destination}`

### 공통 원칙

- 모든 메시지에 `type` 필드 포함 (Enum 기반)
- Request DTO는 `playerId`/`sessionId` 포함 안 함 (Principal에서 추출)
- Response DTO는 상세 정보 포함
- **시간은 Instant (Unix timestamp, milliseconds)** - 게임 타이머 계산 최적화

---

## 메시지 타입

### RoomEventType (방 이벤트)

```java
public enum RoomEventType {
    ROOM_INIT,          // 방 초기화 (입장 시 본인에게만)
    PLAYER_JOINED,      // 플레이어 입장
    PLAYER_LEFT,        // 플레이어 퇴장
    PLAYER_READY,       // 플레이어 준비 완료
    PLAYER_UNREADY,     // 플레이어 준비 취소
    HOST_CHANGED        // 방장 변경
}
```

### GameEventType (게임 이벤트)

```java
public enum GameEventType {
    GAME_START,         // 게임 시작
    ROUND_START,        // 라운드 시작 (퀴즈 출제)
    ANSWER_RECEIVED,    // 답변 접수 확인 (개인)
    ROUND_END,          // 라운드 종료 (정답 공개)
    PLAYER_DIED,        // 플레이어 사망
    PLAYER_SURVIVED,    // 플레이어 생존 (정답)
    GAME_END            // 게임 종료
}
```

### ChatEventType (채팅 이벤트)

```java
public enum ChatEventType {
    CHAT_MESSAGE        // 채팅 메시지
}
```

---

## 방 (Room) 메시지

### 1. ROOM_INIT (방 초기화 - 입장한 본인에게만)

#### Request

```
SEND /app/room/join
```

```json
{
  "roomId": "1234"
}
```

#### Response (Personal - 본인에게만)

```
SUBSCRIBE /queue/room/init
```

```json
{
  "type": "ROOM_INIT",
  "room": {
    "roomId": "1234",
    "roomTitle": "재미있는 퀴즈방",
    "hostId": "session-def-456",
    "capacity": 10,
    "status": "WAITING"
  },
  "players": [
    {
      "id": "session-def-456",
      "nickname": "귀여운사자"
    },
    {
      "id": "session-ghi-789",
      "nickname": "슬픈코끼리"
    },
    {
      "id": "session-abc-123",
      "nickname": "멋진하마"
    }
  ]
}
```

**설명**: 방에 입장한 본인에게만 전송. 방 전체 정보 + 모든 플레이어 목록으로 화면 초기화

---

### 2. PLAYER_JOINED (플레이어 입장 알림)

#### Response (Broadcast - 모두에게)

```
SUBSCRIBE /topic/room/{roomId}
```

```json
{
  "type": "PLAYER_JOINED",
  "player": {
    "id": "session-abc-123",
    "nickname": "멋진하마"
  }
}
```

**설명**: 플레이어가 입장하면 방의 모든 사람에게 브로드캐스트 (변경사항만)

---

### 3. PLAYER_LEFT (플레이어 퇴장)

#### Request

```
SEND /app/room/leave
```

```json
{
  "roomId": "1234"
}
```

#### Response (Broadcast)

```
SUBSCRIBE /topic/room/{roomId}
```

```json
{
  "type": "PLAYER_LEFT",
  "player": {
    "id": "session-abc-123",
    "nickname": "멋진하마"
  },
  "leftAt": 1738825800000
}
```

**설명**: 플레이어가 방을 나가거나 연결이 끊기면 브로드캐스트

---

### 4. PLAYER_READY (플레이어 준비 완료)

#### Request

```
SEND /app/room/ready
```

```json
{
  "roomId": "1234"
}
```

#### Response (Broadcast)

```
SUBSCRIBE /topic/room/{roomId}
```

```json
{
  "type": "PLAYER_READY",
  "player": {
    "id": "session-abc-123",
    "nickname": "멋진하마"
  },
  "allReady": false
}
```

**설명**: 플레이어가 준비 버튼을 누르면 브로드캐스트

---

### 5. PLAYER_UNREADY (플레이어 준비 취소)

#### Request

```
SEND /app/room/unready
```

```json
{
  "roomId": "1234"
}
```

#### Response (Broadcast)

```
SUBSCRIBE /topic/room/{roomId}
```

```json
{
  "type": "PLAYER_UNREADY",
  "player": {
    "id": "session-abc-123",
    "nickname": "멋진하마"
  }
}
```

**설명**: 플레이어가 준비 취소 버튼을 누르면 브로드캐스트

---

### 6. HOST_CHANGED (방장 변경)

#### Response (Broadcast) - 자동 발생

```
SUBSCRIBE /topic/room/{roomId}
```

```json
{
  "type": "HOST_CHANGED",
  "newHost": {
    "id": "session-def-456",
    "nickname": "귀여운사자"
  },
  "previousHostId": "session-abc-123",
  "reason": "HOST_LEFT"
}
```

**설명**: 방장이 나가면 자동으로 다음 플레이어에게 방장 권한 이전

---

## 게임 (Game) 메시지

### 1. GAME_START (게임 시작)

#### Request (호스트만 가능)

```
SEND /app/game/start
```

```json
{
  "roomId": "1234"
}
```

#### Response (Broadcast)

```
SUBSCRIBE /topic/room/{roomId}
```

```json
{
  "type": "ROOM_INIT",
  "room": {
    "roomId": "1234",
    "roomTitle": "재미있는 퀴즈방",
    "hostId": "session-def-456",
    "capacity": 10,
    "status": "WAITING"
  },
  "players": [
    {
      "id": "session-def-456",
      "nickname": "귀여운사자",
      "status": "READY"
    },
    {
      "id": "session-ghi-789",
      "nickname": "슬픈코끼리",
      "status": "NOT_READY"
    },
    {
      "id": "session-abc-123",
      "nickname": "멋진하마",
      "status": "NOT_READY"
    }
  ]
}
```

**설명**: 방장이 게임 시작 버튼을 누르면 게임 시작 알림

---

### 2. ROUND_START (라운드 시작 - 퀴즈 출제)

#### Response (Broadcast) - 자동 발생

```
SUBSCRIBE /topic/room/{roomId}
```

```json
{
  "type": "ROUND_START",
  "gameId": "game-uuid-abc-123",
  "round": 1,
  "quiz": {
    "quizId": "quiz-uuid-def-456",
    "question": "대한민국의 수도는?",
    "choices": [
      {
        "index": 0,
        "text": "서울"
      },
      {
        "index": 1,
        "text": "부산"
      },
      {
        "index": 2,
        "text": "대구"
      },
      {
        "index": 3,
        "text": "인천"
      }
    ],
    "imageUrl": null,
    "timeLimit": 5
  },
  "startTime": 1738825805000,
  "endTime": 1738825810000
}
```

**설명**: 라운드 시작 시 퀴즈 출제 (정답은 포함 안 함)

---

### 3. ANSWER_RECEIVED (답변 접수 확인)

#### Request

```
SEND /app/game/answer
```

```json
{
  "gameId": "game-uuid-abc-123",
  "choiceIndex": 0,
  "submittedAt": 1738825807123
}
```

#### Response (Personal) - 본인에게만

```
SUBSCRIBE /queue/game/{sessionId}
```

```json
{
  "type": "ANSWER_RECEIVED",
  "gameId": "game-uuid-abc-123",
  "round": 1,
  "choiceIndex": 0,
  "receivedAt": 1738825807125
}
```

**설명**: 답변 제출 완료 확인 (결과는 ROUND_END에서 공개)

---

### 4. ROUND_END (라운드 종료 - 정답 공개)

#### Response (Broadcast) - 자동 발생

```
SUBSCRIBE /topic/room/{roomId}
```

```json
{
  "type": "ROUND_END",
  "gameId": "game-uuid-abc-123",
  "round": 1,
  "correctChoiceIndex": 0,
  "correctAnswerText": "서울",
  "results": [
    {
      "player": {
        "id": "session-abc-123",
        "nickname": "멋진하마"
      },
      "choiceIndex": 0,
      "submittedAt": 1738825807123,
      "result": "SURVIVED"
    },
    {
      "player": {
        "id": "session-def-456",
        "nickname": "귀여운사자"
      },
      "choiceIndex": 1,
      "submittedAt": 1738825808456,
      "result": "DIED",
      "deathReason": "WRONG_ANSWER"
    },
    {
      "player": {
        "id": "session-ghi-789",
        "nickname": "슬픈코끼리"
      },
      "choiceIndex": null,
      "submittedAt": null,
      "result": "DIED",
      "deathReason": "TIMEOUT"
    }
  ],
  "endedAt": 1738825810000
}
```

**설명**: 5초 후 또는 모든 플레이어 답변 제출 시 정답 공개 및 생존/사망 결과

**DeathReason 종류:**

- `WRONG_ANSWER`: 오답
- `TIMEOUT`: 시간 초과 (답변 안 함)
- `LATE_SUBMISSION`: 늦은 제출 (5.5초 이후)

---

### 5. PLAYER_DIED (플레이어 사망)

#### Response (Broadcast) - ROUND_END와 함께 발생

```
SUBSCRIBE /topic/room/{roomId}
```

```json
{
  "type": "PLAYER_DIED",
  "gameId": "game-uuid-abc-123",
  "round": 1,
  "player": {
    "id": "session-def-456",
    "nickname": "귀여운사자"
  },
  "deathReason": "WRONG_ANSWER"
}
```

**설명**: 플레이어가 사망했을 때 개별 알림 (UI 애니메이션용)

---

### 6. PLAYER_SURVIVED (플레이어 생존)

#### Response (Personal) - 본인에게만

```
SUBSCRIBE /queue/game/{sessionId}
```

```json
{
  "type": "PLAYER_SURVIVED",
  "gameId": "game-uuid-abc-123",
  "round": 1,
  "message": "정답입니다! 다음 라운드로 진행합니다.",
  "nextRoundStartsIn": 3
}
```

**설명**: 정답을 맞춘 플레이어에게 개인 메시지

---

### 7. GAME_END (게임 종료)

#### Response (Broadcast) - 자동 발생

```
SUBSCRIBE /topic/room/{roomId}
```

```json
{
  "type": "GAME_END",
  "gameId": "game-uuid-abc-123",
  "totalRounds": 10,
  "completedRounds": 7,
  "endReason": "LAST_SURVIVOR",
  "winner": {
    "id": "session-abc-123",
    "nickname": "멋진하마"
  },
  "rankings": [
    {
      "rank": 1,
      "player": {
        "id": "session-abc-123",
        "nickname": "멋진하마"
      },
      "survivedRounds": 7
    },
    {
      "rank": 2,
      "player": {
        "id": "session-def-456",
        "nickname": "귀여운사자"
      },
      "survivedRounds": 5
    }
  ],
  "endedAt": 1738826100000
}
```

**설명**: 게임 종료 (1명 생존 또는 10라운드 완료)

**EndReason 종류:**

- `LAST_SURVIVOR`: 마지막 1명 생존
- `ALL_ROUNDS_COMPLETED`: 10라운드 완료
- `ALL_PLAYERS_DIED`: 모든 플레이어 사망 (무승부)
- `HOST_ABORTED`: 방장이 게임 강제 종료

---

## 채팅 (Chat) 메시지

### 1. CHAT_MESSAGE (채팅 메시지)

#### Request

```
SEND /app/chat/send
```

```json
{
  "roomId": "1234",
  "message": "안녕하세요!"
}
```

#### Response (Broadcast)

```
SUBSCRIBE /topic/room/{roomId}/chat
```

```json
{
  "type": "CHAT_MESSAGE",
  "sender": {
    "id": "session-abc-123",
    "nickname": "멋진하마"
  },
  "message": "안녕하세요!",
  "sentAt": 1738825800000
}
```

**설명**: 방 내 채팅 메시지

---

## 에러 메시지

### 에러 응답 (Personal)

```
SUBSCRIBE /queue/errors
```

```json
{
  "code": "ROOM_FULL",
  "message": "방 인원이 가득 찼습니다."
}
```

**주요 에러 코드:**

- `ROOM_NOT_FOUND`: 방을 찾을 수 없음
- `ROOM_FULL`: 방 인원 초과
- `ROOM_IN_GAME`: 게임 진행 중
- `NOT_HOST`: 방장 권한 필요
- `INVALID_GAME_STATE`: 잘못된 게임 상태
- `ALREADY_SUBMITTED`: 이미 답변 제출함
- `TIMEOUT`: 제출 시간 초과

---

## 구현 우선순위

### Phase 1 (MVP)

1. ✅ ROOM_INIT (방 초기화)
2. ✅ PLAYER_JOINED (입장 알림)
3. ⬜ PLAYER_LEFT
4. ⬜ GAME_START
5. ⬜ ROUND_START
6. ⬜ ANSWER_RECEIVED (답변 제출)
7. ⬜ ROUND_END
8. ⬜ GAME_END

### Phase 2 (확장)

8. ⬜ PLAYER_READY / PLAYER_UNREADY
9. ⬜ HOST_CHANGED
10. ⬜ PLAYER_DIED (개별 알림)
11. ⬜ PLAYER_SURVIVED (개인 메시지)

### Phase 3 (선택)

12. ⬜ CHAT_MESSAGE
13. ⬜ ROOM_SETTINGS_CHANGED

---

## 부록 A: Instant (Unix Timestamp) 사용 가이드

### 시간 계산 예시

#### 클라이언트 (JavaScript)

```javascript
// 남은 시간 계산 (밀리초)
const remainingMs = data.endTime - Date.now();

// 남은 시간 (초)
const remainingSec = Math.floor(remainingMs / 1000);

// 실시간 타이머
const timer = setInterval(() => {
    const remaining = Math.max(0, data.endTime - Date.now());
    const seconds = Math.ceil(remaining / 1000);
    updateTimerUI(seconds);

    if (remaining <= 0) {
        clearInterval(timer);
    }
}, 100);

// 제출 시간 생성
const submittedAt = Date.now();  // 1738825807123
```

#### 서버 (Java)

```java
// 현재 시간 (Unix timestamp)
long now = Instant.now().toEpochMilli();  // 1738825807123

// 시간 차이 계산
long diff = submittedAt - roundEndTime;

// 500ms 오차 허용 체크
if(diff >500){
        return DeathReason.LATE_SUBMISSION;
}

// 5초 후 시간
long endTime = Instant.now().toEpochMilli() + 5000;
```

---

## 부록 B: 클라이언트 사용 예시

### JavaScript (SockJS + STOMP)

```javascript
// 연결
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
    console.log('Connected:', frame);

    // 방 초기화 메시지 구독 (본인만)
    stompClient.subscribe('/queue/room/init', (message) => {
        const data = JSON.parse(message.body);
        if (data.type === 'ROOM_INIT') {
            initializeRoom(data.room, data.players);
        }
    });

    // 방 이벤트 구독 (모두)
    stompClient.subscribe('/topic/room/1234', (message) => {
        const data = JSON.parse(message.body);

        switch (data.type) {
            case 'PLAYER_JOINED':
                addPlayer(data.player);
                break;

            case 'PLAYER_LEFT':
                removePlayer(data.player);
                break;

            case 'ROUND_START':
                displayQuiz(data.quiz);
                startTimer(data.startTime, data.endTime);
                break;

            case 'ROUND_END':
                showResults(data.results);
                break;

            case 'GAME_END':
                showRankings(data.rankings);
                break;
        }
    });

    // 개인 메시지 구독
    stompClient.subscribe('/queue/game/' + sessionId, (message) => {
        const data = JSON.parse(message.body);

        if (data.type === 'ANSWER_RECEIVED') {
            showSubmitConfirmation();
        }
    });

    // 에러 구독
    stompClient.subscribe('/queue/errors', (message) => {
        const error = JSON.parse(message.body);
        alert(`오류: ${error.message}`);
    });
});

// 방 참가
function joinRoom(roomId) {
    stompClient.send('/app/room/join', {}, JSON.stringify({
        roomId: roomId
    }));
}

// 답변 제출
function submitAnswer(gameId, choiceIndex) {
    stompClient.send('/app/game/answer', {}, JSON.stringify({
        gameId: gameId,
        choiceIndex: choiceIndex,
        submittedAt: Date.now()  // Unix timestamp (ms)
    }));
}
```

---

## 변경 이력

| 날짜         | 버전  | 변경 내용                                                                   |
|------------|-----|-------------------------------------------------------------------------|
| 2026-02-06 | 1.2 | Personal + Broadcast 방식으로 변경, 불필요한 필드 제거 (currentPlayerCount, roomId 등) |
| 2026-02-06 | 1.1 | 시간 형식을 ZonedDateTime → Instant로 변경 (게임 타이머 최적화)                         |
| 2026-02-06 | 1.0 | 초안 작성                                                                   |
