# E2E 테스트 메모

## 대기실 진입 후 반드시 heading 확인

방 생성/입장 후 테스트를 바로 끝내면 WebSocket CONNECT가 완료되기 전에 연결이 끊겨 서버가 disconnect를 감지하지 못하고 쓰레기 방이 남는다.

`room_name_heading`(h1)은 `connectWebSocket()` 완료 후에만 렌더링되므로, 아래 한 줄이 STOMP 연결 완료를 보장한다.

```python
rooms_page.create_room(title)
expect(WaitingRoomPage(page).room_name_heading).to_be_visible()  # 필수
```

## 게임 시작 후 화면 확인

게임 시작 직후에는 문제 화면 또는 종료 화면으로 전환될 수 있으므로, `GamePage.wait_until_loaded()`로 먼저 화면 전환을 동기화한다.

```python
game_page = GamePage(page)
waiting_page.click_start()
game_page.wait_until_loaded()
```

## 결과 화면 확인

게임 진행 완료까지 대기 시간이 길 수 있으므로(문항 타이머 누적), 결과 모달은 `GameResultPage`로 확인하고 넉넉한 timeout을 사용한다.

```python
game_page.click_answer(1)  # 플레이어 1만 제출
result_page = GameResultPage(page)
result_page.wait_until_visible(timeout=90_000)
```
