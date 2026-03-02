# 테스트 코드 구현 원칙

이 문서는 정합성/동시성 테스트 구현 시 따를 기준 문서(SSOT)다.  
기존 가이드와 충돌하는 내용이 있으면 이 문서를 우선한다.

## 1. 구조 원칙

1. `@Nested`로 시나리오를 묶는다.
2. 테스트 메서드명은 한글 설명형 + underscore 스타일을 사용한다.
3. 모든 테스트는 `given / when / then` 블록을 고정한다.
4. 테스트 세팅은 분리한다.
   - 객체 초기화: `@BeforeEach setup`
   - 테스트 데이터 생성: `loginGuest`, fixture helper

## 2. 동시성 재현 원칙

1. 공통 실행 코드는 `runConcurrently(...)` 헬퍼로 추출한다.
2. 동시성 재현은 `ready / start / done` 래치 패턴으로 시작 시점을 맞춘다.
3. 비즈니스 로직 검증은 서비스 테스트에서 수행하고, 동시성 재현 제어는 서비스 로직 밖(테스트 헬퍼)에서 수행한다.

## 3. 검증 원칙

1. 테스트 1개는 개념 1개만 검증한다.
2. `assert`는 핵심 불변식만 남긴다.
3. 비결정적 결과(누가 먼저 성공했는지)는 고정하지 않는다.
4. 최종 불변식(정원, 유일성, 정합성) 중심으로 검증한다.
5. 성공 응답만 보지 않고 저장소 상태까지 확인한다.
   - 예: `roomRepository.findAll()`, `playerRepository.findAll()`
6. 합격/실패 판정은 반드시 `AssertJ` 기반 assertion으로 판단한다.
7. 로그는 디버깅 용도이며, 테스트 판정 근거로 사용하지 않는다.

## 4. 코드 템플릿

```java
@Nested
class RoomCreateJoinConcurrencyTests {

    @Test
    void 동시에_여러_사용자가_입장해도_방_정원을_넘겨_입장되지_않는다() throws InterruptedException {
        // given
        long hostUserId = loginGuest("host").userId();
        String roomId = roomService.createRoom("테스트 방", 2, hostUserId).roomId();
        List<Long> userIds = ...;

        // when
        runConcurrently(userIds, userId -> roomService.joinRoom(roomId, userId));

        // then
        assertThat(playerRepository.findAllByRoomId(roomId)).hasSize(2);
    }
}
```

## 5. 권장 네이밍 패턴

테스트명은 비즈니스 관점으로 작성한다.

- `~해도_~된다`: 정합성 불변식 검증
- `~해도_~되지_않는다`: 초과/중복/누수 방지 검증
- `~요청이_동시에_도착해도_~가_일치한다`: 스냅샷 정합성 검증

예시:

- `동시에_여러_사용자가_입장해도_방_정원을_넘겨_입장되지_않는다`
- `방장이_나가는_순간_게임_시작_요청이_겹쳐도_새_방장이_정상적으로_결정된다`

## 6. AssertJ 사용 규칙

테스트 판정은 AssertJ로만 한다.

```java
assertThat(players).hasSize(2);
assertThat(players).extracting(Player::getUserId).doesNotHaveDuplicates();
```

예외 코드 검증이 필요한 테스트는 타입 + 코드까지 확인한다.

```java
assertThatThrownBy(() -> roomService.joinRoom(roomId, userId))
        .isInstanceOf(CustomException.class)
        .matches(e -> ((CustomException) e).getErrorCode().getCode()
                .equals(RoomErrorCode.ROOM_FULL.name()));
```

## 7. 테스트 헬퍼 작성 기준

중복되는 패턴은 헬퍼로 추출한다.

- `runConcurrently(...)`: 동시 실행 제어
- `loginGuest(...)`: 사용자 생성
- `createAndSaveXxx(...)`: fixture 생성

헬퍼 네이밍 규칙:

- `createXxx`: 객체 생성만
- `saveXxx`: 저장만
- `createAndSaveXxx`: 생성 + 저장

## 8. 테스트 격리 원칙

1. 각 테스트는 독립 실행 가능해야 한다.
2. 테스트 간 순서 의존을 두지 않는다.
3. 공유 상태를 피하고 `@BeforeEach`로 매번 초기화한다.

## 9. 구현 체크리스트

- [ ] `@Nested`로 시나리오가 분리되어 있는가
- [ ] 테스트명이 한글 설명형 underscore 스타일인가
- [ ] `given / when / then` 블록이 고정되어 있는가
- [ ] 동시성 실행 코드가 `runConcurrently(...)`로 추출되어 있는가
- [ ] `ready / start / done` 래치 패턴으로 시작 시점을 맞췄는가
- [ ] 테스트가 한 가지 개념만 검증하는가
- [ ] 누가 먼저 성공하는지 같은 비결정적 순서를 assert하지 않는가
- [ ] 응답 + 저장소 상태를 함께 검증하는가
- [ ] 판정은 AssertJ assert만으로 내리고 있는가
