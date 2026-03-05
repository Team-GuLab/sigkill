# E2E 테스트 메모

## 대기실 진입 후 반드시 heading 확인

방 생성/입장 후 테스트를 바로 끝내면 WebSocket CONNECT가 완료되기 전에 연결이 끊겨 서버가 disconnect를 감지하지 못하고 쓰레기 방이 남는다.

`room_name_heading`(h1)은 `connectWebSocket()` 완료 후에만 렌더링되므로, 아래 한 줄이 STOMP 연결 완료를 보장한다.

```python
rooms_page.create_room(title)
expect(WaitingRoomPage(page).room_name_heading).to_be_visible()  # 필수
```