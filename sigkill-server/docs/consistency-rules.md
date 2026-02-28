작성자: 이상현, 260227

# 정합성 규칙 문서

## 1. 목적

실시간 퀴즈 게임에서 여러 유저의 동시 요청에도 핵심 도메인 규칙 (퀴즈 결과, 방 인원, 게임 상태 등)이 예측 가능하게 동작하도록 하는 정합성 규칙을 정의합니다.

## 2. 도메인 규칙

### 예시
도메인 규칙을 정합성 관점(불변조건/상태전이/원자성)으로 명세해야 합니다.


### 방

- 방의 최소 인원은 1명이다.

#### 방 생성 (createRoom)

- 방 생성 시 `roomId` 는 고유해야 한다.
- 방을 생성한 플레이어는 입장 상태여야 한다.

#### 방 입장 (joinRoom)

- `방 입장` 을 여러명의 유저가 동시에 요청할 경우, 먼저 요청을 보낸 유저가 방에 입장한다.

#### 방 나가기 (leaveRoom)

- `LEAVE` 요청은 `roomId` 락 내에서만 수행한다.
- 충돌 예시:
  - `joinRoom`: 입장 성공 응답 직후 방이 삭제되어 고아 상태가 생길 수 있음
  - `startGame`: 게임 참가자와 실제 방 인원이 불일치할 수 있음
  - `hostChange`: 호스트 변경과 퇴장 처리가 교차되어 hostId가 비어 있는 상태가 생길 수 있음
- PostCondition:
- LEAVE 성공 후 요청 유저는 해당 roomId의 Player 집합에 존재하면 안 된다.
- LEAVE 성공 후 해당 roomId의 Player가 0명이면 Room은 삭제되어야 한다.
- LEAVE 성공 후 Player가 1명 이상 남아 있고, 나간 유저가 host였다면 - changeHost가 정확히 1회 수행되어야 한다.

#### 호스트 변경 (changeHost)

- `LEAVE` 요청(`/app/room/leave`) 기반의 `changeHost` 처리는 `roomId` 락 내에서 수행한다.
- 충돌 예시:
  - `leaveRoom`: 명시적 퇴장과 DISCONNECT 자동 퇴장이 교차되면 새 호스트 선출/갱신 결과가 비결정적일 수 있음
  - `startGame`: 게임 시작의 호스트 권한 검사와 `Room.hostId` 갱신이 교차되면 시작 권한 판단이 엇갈릴 수 있음
- 추가 정보:
  - 방장 권한의 단일 소스는 `Room.hostId`이며, 락 내부에서 호스트 선출(`createdAt` 최소)과 `hostId` 갱신을 원자적으로 완료한 뒤 `HOST_CHANGED`(자동 이벤트)를 전송해야 한다. 단, 마지막 1명 퇴장으로 방이 삭제되는 경우 `HOST_CHANGED`는 전송하지 않는다.


#### 방 준비 (readyPlayer)

- `READY` 요청(`/app/room/ready`)은 `roomId` 락 내에서 수행한다.
- 충돌 예시:
  - `startGame`: 준비 상태 반영과 시작 조건 검사(모든 게스트 READY)가 교차되면 게임 시작 허용 여부가 비결정적으로 보일 수 있음
  - `leaveRoom`: READY 처리 중 퇴장이 교차되면 `allReady` 계산 대상 플레이어 집합이 달라져 이벤트와 실제 상태가 어긋날 수 있음
- 추가 정보:
  - 락 내부에서 `host 아님`/`INGAME 아님` 검증, `readyStatus=READY` 반영, `allReady` 계산까지 원자적으로 완료하고 `PLAYER_READY` 이벤트를 생성한다.

#### 방 준비 해제 (unreadyPlayer)

- `UNREADY` 요청(`/app/room/unready`)은 `roomId` 락 내에서 수행한다.
- 충돌 예시:
  - `startGame`: 준비 해제와 시작 조건 검사가 교차되면 실제로는 NOT_READY인데 시작이 허용되는 불일치가 생길 수 있음
  - `changeHost`: 호스트 퇴장에 따른 호스트 교체와 준비 해제가 교차되면 role/ready 상태 해석이 엇갈릴 수 있음
- 추가 정보:
  - 락 내부에서 `host 아님`/`INGAME 아님` 검증과 `readyStatus=NOT_READY` 반영까지 완료하고 `PLAYER_UNREADY` 이벤트를 생성한다.

### 게임

#### 게임 시작 (startGame)

- `START` 요청(`/app/room/start`)은 `roomId` 락 내에서 수행한다.
- 충돌 예시:
  - `readyPlayer/unreadyPlayer`: 준비 상태 변경과 시작 조건 검사(모든 게스트 READY)가 교차되면 시작 허용 여부가 요청마다 달라질 수 있음
  - `leaveRoom/changeHost`: 인원/호스트 변경과 게임 생성이 교차되면 `GAME_START` 참가자 스냅샷과 실제 방 상태가 불일치할 수 있음
- 추가 정보:
  - `gameId`가 생성되기 전 단계이므로 `roomId`를 기준으로 `검증(호스트/인원/READY) -> Game/GamePlayer 생성 -> Room.status=INGAME`까지 원자적으로 완료하고, `GAME_START` 브로드캐스트는 상태 확정 이후 수행한다.

#### 게임 로드 (loadGame)

- `LOAD` 요청(`/app/game/load`)은 `gameId` 락 내에서 수행한다.
- 충돌 예시:
  - `loadGame`: 여러 사용자의 동시 로드 완료 반영이 교차되면 `players[*].isLoaded` 및 `allLoaded` 계산 결과가 비결정적으로 보일 수 있음
  - `endGame`: 게임 종료 정리(게임/플레이어 삭제)와 로드 완료 반영이 교차되면 이미 종료된 게임에 대해 `GAME_LOADED`가 생성될 수 있음
- 추가 정보:
  - 락 내부에서 `참가자 검증 -> isLoaded 반영 -> allLoaded 계산`을 원자적으로 끝내고 `GAME_LOADED`를 생성해야 하며, `allLoaded=true`일 때의 첫 `QUIZ_START` 스케줄링은 게임당 1회만 보장되어야 한다.

#### 퀴즈 시작 (startQuiz)

- `QUIZ_START` 처리 요청(자동 스케줄 실행 포함)은 `gameId` 락 내에서 수행한다.
- 충돌 예시:
  - `submitChoice`: `currentQuizIndex` 증가와 선택지 번호 매핑 저장이 교차되면, 새 퀴즈 제출이 매핑 조회 실패 또는 잘못된 퀴즈 판정으로 이어질 수 있음
  - `endQuiz`: 라운드 종료 처리와 다음 퀴즈 시작이 교차되면 퀴즈 인덱스가 중복 증가하거나 라운드 경계가 꼬일 수 있음
- 추가 정보:
  - 락 내부에서 `게임 진행 상태 검증 -> 다음 퀴즈 인덱스/시작시각 확정 -> 선택지 셔플/번호매핑 저장 -> QUIZ_START 이벤트 생성`까지 원자적으로 완료하고, 브로드캐스트는 상태 확정 이후 수행한다.

#### 답 제출 (submitChoice)

- `SUBMIT` 요청(`/app/game/submit`)은 `gameId` 락 없이 처리하고, `roundOpen=true`인 현재 라운드에서만 반영한다.
- 충돌 예시:
  - `submitChoice`: 동일 유저의 연속 제출이 교차되면 최종 제출 선택지와 저장 순서가 비결정적으로 보일 수 있음
  - `endQuiz`: 제출 처리와 라운드 종료가 교차되면 마감 경계에서 점수 집계 대상이 요청마다 달라질 수 있음
- 추가 정보:
  - `게임 진행 상태/현재 퀴즈/마감시간` 및 `roundOpen` 검증을 통과한 요청만 `(gameId, quizId, userId)` 키에 원자적으로 덮어쓰기 저장한다. 유효한 제출 중 유저별 마지막 저장값을 최종 제출로 사용한다.

#### 퀴즈 종료 (endQuiz)

- `QUIZ_END` 처리 요청(자동 스케줄 실행 포함)은 `gameId` 락 내에서 수행한다.
- 충돌 예시:
  - `submitChoice`: 마감 경계의 제출 저장과 퀴즈 종료 채점이 교차되면 점수/생존 판정 대상이 비결정적으로 달라질 수 있음
- 추가 정보:
  - 락 획득 직후 `roundOpen=false`(제출창 close)를 먼저 확정한 뒤, `현재 퀴즈 검증 -> 제출 스냅샷 채점 -> 점수/생존 상태 반영 -> 종료 조건 판정(필요 시 endGame) -> 라운드 제출/매핑 데이터 정리` 순서로 원자 처리하고 `QUIZ_END`(및 조건 충족 시 `GAME_END`) 이벤트를 생성한다.

#### 게임 종료 (endGame)

- `GAME_END` 처리 요청(서버 내부 `endGame` 호출 포함)은 `gameId` 락 내에서 수행한다.
- 충돌 예시:
  - `loadGame`: 로딩 완료 반영과 게임 데이터 삭제가 교차되면 `GAME_LOADED` 계산 대상이 사라져 이벤트와 실제 상태가 불일치할 수 있음
  - `submitChoice`: 제출 저장과 `SelectedChoice`/매핑/`GamePlayer` 삭제가 교차되면 제출 반영이 유실되거나 종료 직후 오류가 발생할 수 있음
- 추가 정보:
  - 락 내부에서 `종료 사유 산정 -> 랭킹 계산 -> GAME_END 이벤트 생성 -> room 상태 WAITING 복귀 -> game 관련 저장소 정리 -> 방 플레이어 ready 초기화`까지 원자적으로 완료한다.
