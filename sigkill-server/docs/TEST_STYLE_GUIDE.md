# 테스트 코드 스타일 가이드

## 목차

1. [기본 원칙](#기본-원칙)
2. [테스트 구조](#테스트-구조)
3. [네이밍 컨벤션](#네이밍-컨벤션)
4. [Given-When-Then 패턴](#given-when-then-패턴)
5. [테스트 데이터 관리](#테스트-데이터-관리)
6. [Assertion 스타일](#assertion-스타일)
7. [예외 테스트](#예외-테스트)
8. [테스트 범위](#테스트-범위)
9. [헬퍼 메소드](#헬퍼-메소드)
10. [테스트 격리](#테스트-격리)

---

## 기본 원칙

### 테스트는 문서다

- 테스트 코드는 해당 기능의 동작 방식을 설명하는 살아있는 문서입니다
- 다른 개발자가 읽고 이해할 수 있도록 명확하게 작성합니다
- 메서드명을 한글로 작성하여 테스트 의도를 명확히 표현합니다

### 독립성과 격리성

- 각 테스트는 독립적으로 실행 가능해야 합니다
- 테스트 간 실행 순서에 의존하지 않습니다
- 공유 상태를 피하고 필요시 `@BeforeEach`로 초기화합니다

---

## 테스트 구조

### @Nested를 활용한 계층적 구조

기능별로 테스트를 그룹화하여 가독성을 높입니다:

```java
@DataJpaTest
class MemberServiceTest {

    @Nested
    class 회원_조회_기능 {

        @Test
        void 이메일로_회원_조회에_성공한다() {
            // 테스트 코드
        }

        @Test
        void 존재하지_않는_이메일로_조회_시_예외가_발생한다() {
            // 테스트 코드
        }
    }

    @Nested
    class 회원_저장_기능 {
        // 저장 관련 테스트들
    }
}
```

**장점:**

- 관련된 테스트들을 논리적으로 그룹화
- 테스트 리포트의 가독성 향상
- 각 그룹별로 공통 setup이 필요한 경우 분리 가능

---

## 네이밍 컨벤션

### 테스트 메소드명

**형식:** `[기능]_[조건]_[결과]` 또는 `[기능에_성공한다]` 스타일

메서드명을 한글로 작성하여 테스트 의도를 명확히 표현합니다. 가독성을 위해 언더스코어로 단어를 구분합니다.

```java
// 좋은 예시
@Test
void 이메일로_회원_조회에_성공한다()

@Test
void 중복된_ID로_저장_시_예외가_발생한다()

@Test
void 페이징이_정상적으로_작동한다()

@Test
void 이미_가족_멤버인_사용자로_초대코드_생성_시_예외가_발생한다()
```

**권장 패턴:**

- `~에_성공한다`: 정상 동작하는 경우
- `~를_찾을_수_없을_때_예외가_발생한다`: 리소스를 찾을 수 없는 경우
- `~_시_예외가_발생한다`: 예외가 발생하는 경우
- `~이_정상적으로_작동한다`: 특정 기능이 정상 동작하는지 확인
- `~을_반환한다`: 빈 결과를 반환하는 경우

---

## Given-When-Then 패턴

모든 테스트는 Given-When-Then 구조를 따르며, 주석으로 명확히 구분합니다:

```java
@Test
void 사진을_정상적으로_조회한다() {
    // Given - 테스트 준비 단계
    Member member = saveTestMember();
    Family family = createAndSaveFamilyWithMember(member);
    Photo photo = createAndSavePhoto(family, member);

    // When - 테스트 실행 단계
    PhotoDTO result = photoService.getPhoto(family.getId(), photo.getId(), member.getUid());

    // Then - 결과 검증 단계
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(photo.getId());
    assertThat(result.urlString()).isEqualTo(photo.getUrl());
}
```

### 각 단계별 가이드

**Given (준비):**

- 테스트에 필요한 데이터와 상태를 준비
- 헬퍼 메소드를 활용하여 간결하게 작성
- 필요한 경우 변수에 설명적인 이름 사용

**When (실행):**

- 테스트하려는 실제 동작을 수행
- 가능한 한 줄로 표현
- 결과값이 있다면 명확한 변수명에 저장

**Then (검증):**

- 예상한 결과가 나왔는지 검증
- 여러 assertion을 사용하여 다양한 측면을 검증
- 필요한 경우 DB나 외부 상태도 확인

---

## 테스트 데이터 관리

### 고정 상수 정의

테스트에서 반복적으로 사용되는 값은 상수로 정의:

```java
class MemberServiceTest {

    // 테스트용 고정 데이터
    private static final String TEST_UID = "test-uid-123";
    private static final String TEST_NAME = "테스트이름";
    private static final String TEST_EMAIL = "test@example.com";
    private static final Role TEST_ROLE = Role.PARENT;

    // 테스트 메소드들...
}
```

**장점:**

- 테스트 데이터의 일관성 유지
- 변경이 필요할 때 한 곳만 수정
- 테스트 의도가 더 명확해짐

### 팩토리 메소드 패턴

테스트 객체 생성을 위한 헬퍼 메소드 작성:

```java
// 기본 생성자
private Member createTestMember() {
    return Member.builder()
            .uid(TEST_UID)
            .name(TEST_NAME)
            .email(TEST_EMAIL)
            .role(TEST_ROLE)
            .build();
}

// 파라미터를 받는 오버로딩 버전
private Member createTestMember(String uid, String name, String email, Role role) {
    return Member.builder()
            .uid(uid)
            .name(name)
            .email(email)
            .role(role)
            .build();
}

// 생성과 저장을 함께 하는 버전
private Member saveTestMember() {
    return memberRepository.save(createTestMember());
}
```

**네이밍 규칙:**

- `createXxx()`: 객체만 생성
- `saveXxx()`: 생성 후 저장까지
- `createAndSaveXxx()`: 생성 후 저장을 명시적으로 표현

---

## Assertion 스타일

### AssertJ 라이브러리 사용

JUnit의 기본 assertion보다 AssertJ를 사용하여 가독성 향상:

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 좋은 예시 (AssertJ)
assertThat(result).isNotNull();
assertThat(result.id()).isEqualTo(TEST_UID);
assertThat(result.email()).isEqualTo(TEST_EMAIL);

// 피해야 할 예시 (JUnit)
assertNotNull(result);
assertEquals(TEST_UID, result.id());
```

### 메소드 체이닝

여러 assertion을 체이닝하여 간결하게 표현:

```java
// 컬렉션 검증
assertThat(response.names())
    .hasSize(2)
    .contains(member1.getName(), member2.getName());

// null 및 빈 값 검증
assertThat(dto.connectedTo())
    .isNotNull()
    .isEmpty();
```

### 자주 사용되는 Assertion

```java
// 동등성 검증
assertThat(actual).isEqualTo(expected);
assertThat(actual).isNotEqualTo(unexpected);

// null 검증
assertThat(actual).isNull();
assertThat(actual).isNotNull();

// boolean 검증
assertThat(condition).isTrue();
assertThat(condition).isFalse();

// 숫자 검증
assertThat(count).isZero();
assertThat(count).isPositive();
assertThat(value).isGreaterThan(10);

// 컬렉션 검증
assertThat(list).isEmpty();
assertThat(list).hasSize(5);
assertThat(list).contains(element);
assertThat(list).doesNotContain(element);

// 시간 검증
assertThat(timestamp).isAfter(before);
assertThat(timestamp).isBefore(after);
```

---

## 예외 테스트

### 기본 예외 테스트

`assertThatThrownBy`를 사용하여 예외 검증:

```java
@Test
void 존재하지_않는_이메일로_조회_시_예외가_발생한다() {
    // Given
    String nonExistentEmail = "notfound@example.com";

    // When & Then
    assertThatThrownBy(() -> memberService.findMember(nonExistentEmail, null))
            .isInstanceOf(CustomException.class);
}
```

### 커스텀 에러 코드까지 검증

예외 타입뿐만 아니라 구체적인 에러 코드도 검증:

```java
@Test
void 이름이_2자_미만인_경우_예외가_발생한다() {
    // Given
    String shortName = "김";
    String newUid = "new-uid-short";
    String newEmail = "short@example.com";

    // When & Then
    assertThatThrownBy(() -> memberService.saveMember(shortName, newUid, newEmail))
            .isInstanceOf(CustomException.class)
            .matches(e -> ((CustomException) e).getErrorCode().getCode()
                    .equals(MemberCustomErrorCode.INVALID_NAME_LENGTH.getCode()));
}
```

### 예외 메시지 검증

```java
assertThatThrownBy(() -> service.doSomething())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid input")
        .hasMessageContaining("Invalid");
```

---

## 테스트 범위

### 성공 케이스 (Happy Path)

모든 기능에 대해 정상 동작하는 경우를 반드시 테스트:

```java
@Test
void 회원을_정상적으로_저장한다() {
    // Given
    String newUid = "new-uid-456";
    String newName = "새멤버";
    String newEmail = "new@example.com";

    // When
    MemberDTO savedDTO = memberService.saveMember(newName, newUid, newEmail);

    // Then
    assertThat(savedDTO).isNotNull();
    assertThat(savedDTO.id()).isEqualTo(newUid);

    // DB에서 실제로 저장되었는지 확인
    Member savedMember = memberRepository.findById(newUid).orElse(null);
    assertThat(savedMember).isNotNull();
}
```

### 실패 케이스 (Error Path)

예상 가능한 모든 실패 시나리오를 테스트:

```java
@Nested
class 회원_저장_기능 {

    @Test
    void 새로운_회원_정보_저장에_성공한다() { /* ... */ }

    @Test
    void 이미_존재하는_ID로_저장_시_예외가_발생한다() { /* ... */ }

    @Test
    void 이미_존재하는_이름으로_저장_시_예외가_발생한다() { /* ... */ }

    @Test
    void 이름이_2자_미만인_경우_예외가_발생한다() { /* ... */ }

    @Test
    void 이름이_12자를_초과하는_경우_예외가_발생한다() { /* ... */ }
}
```

### 경계값 테스트 (Boundary Test)

입력값의 경계 조건을 테스트:

```java
@Test
void 이름이_2자에서_12자_사이인_경우_정상적으로_저장된다() {
    // 최소값(2자), 최대값(12자), 그 사이 값 테스트
}

@Test
void 페이징이_정상적으로_작동한다() {
    // 첫 페이지, 중간 페이지, 마지막 페이지 테스트
}
```

### 특수값 테스트 (Edge Case)

null, 빈 문자열, 빈 컬렉션 등 특수한 경우를 테스트:

```java
@Test
void 특수값이_있는_회원도_정상적으로_DTO로_변환된다() {
    // Given
    Member specialMember = Member.builder()
            .uid("special-uid")
            .name("")  // 빈 문자열
            .email(null)  // null 이메일
            .role(null)   // null 역할
            .build();
    memberRepository.save(specialMember);

    // When
    MemberDTO dto = memberService.findMemberById("special-uid");

    // Then
    assertThat(dto.name()).isEmpty();
    assertThat(dto.email()).isNull();
}
```

---

## 헬퍼 메소드

### 중복 제거

반복되는 검증 로직은 헬퍼 메소드로 추출:

```java
// 헬퍼 메소드
private void verifyAllReactionCountsAreZero(ReactionDTO reactionDTO) {
    assertThat(reactionDTO).isNotNull();
    assertThat(reactionDTO.love()).isZero();
    assertThat(reactionDTO.fire()).isZero();
    assertThat(reactionDTO.star()).isZero();
    assertThat(reactionDTO.like()).isZero();
}

// 사용
@Test
void 가족_정보를_정상적으로_조회한다() {
    // Given & When
    FamilyResponse response = familyService.getFamily(family.getId(), member.getUid());

    // Then
    assertThat(response).isNotNull();
    verifyAllReactionCountsAreZero(response.reactionsCount());
}
```

### 체이닝 가능한 헬퍼 메소드

복잡한 테스트 시나리오를 위한 빌더 스타일:

```java
private Family createAndSaveFamilyWithMember(Member member) {
    Family family = new Family();
    family.addMember(member);
    return familyRepository.save(family);
}

// 사용
Member member = saveTestMember();
Family family = createAndSaveFamilyWithMember(member);
Photo photo = createAndSavePhoto(family, member);
```

---

## 테스트 격리

### @BeforeEach를 통한 초기화

각 테스트 전에 데이터를 초기화하여 테스트 간 격리 보장:

```java
@BeforeEach
void setup() {
    memberRepository.deleteAll();
    familyRepository.deleteAll();
    photoRepository.deleteAll();
}
```

### Service 초기화

필요한 경우 Service 객체도 매번 새로 생성:

```java
private MemberService memberService;

@Autowired
private MemberRepository memberRepository;

@BeforeEach
void setup() {
    memberService = new MemberService(memberRepository);
}
```

### 테스트 간 의존성 제거

- 테스트는 어떤 순서로 실행되어도 같은 결과를 보장해야 합니다
- static 변수나 공유 상태를 사용하지 않습니다
- 각 테스트는 자체적으로 필요한 데이터를 생성합니다

---

## 추가 권장사항

### 1. 한 가지만 테스트하기

각 테스트 메소드는 하나의 시나리오만 검증:

```java
// 좋은 예시 - 각각 분리
@Test
void 회원_저장에_성공한다() { /* 저장 성공 */ }

@Test
void 중복된_ID로_저장_시_예외가_발생한다() { /* 중복 ID 실패 */ }

// 나쁜 예시 - 여러 시나리오 혼재
@Test
void 회원_저장의_모든_케이스를_테스트한다() {
    // 성공 케이스와 실패 케이스를 한 테스트에 모두 포함
}
```

### 2. 테스트 메소드 순서

관례적으로 다음 순서로 작성:

1. 필드 선언
2. 고정 상수
3. 헬퍼 메소드
4. @BeforeEach / @AfterEach
5. @Nested 클래스들
    - 성공 케이스를 먼저
    - 실패 케이스를 나중에

### 3. 주석 최소화

코드 자체가 설명적이어야 하며, 불필요한 주석은 피합니다:

```java
// 나쁜 예시
@Test
void 테스트() {
    // 멤버를 생성한다
    Member member = new Member();
    // 멤버를 저장한다
    memberRepository.save(member);
    // 결과를 확인한다
    assertThat(member.getId()).isNotNull();
}

// 좋은 예시
@Test
void 멤버를_정상적으로_저장한다() {
    // Given
    Member member = createTestMember();

    // When
    Member saved = memberRepository.save(member);

    // Then
    assertThat(saved.getId()).isNotNull();
}
```

### 4. 가독성 우선

- 성능보다 가독성을 우선시합니다
- 테스트 코드는 프로덕션 코드보다 더 읽기 쉬워야 합니다
- 복잡한 로직은 헬퍼 메소드로 추출합니다

### 5. 테스트 커버리지

- 모든 public 메소드는 테스트합니다
- private 메소드는 public 메소드를 통해 간접적으로 테스트됩니다
- 중요한 비즈니스 로직은 100% 커버리지를 목표로 합니다

---

## 체크리스트

새로운 테스트를 작성할 때 다음 사항을 확인하세요:

- [ ] 메서드명이 한글로 명확하게 표현되어 있는가? (예: `메서드명에_성공한다`)
- [ ] Given-When-Then 패턴을 따르고 있는가?
- [ ] 테스트 메소드명이 규칙을 따르는가? (`[기능]_[조건]_[결과]`)
- [ ] 성공 케이스와 실패 케이스를 모두 다루는가?
- [ ] 경계값과 특수값을 테스트하는가?
- [ ] 각 테스트는 독립적으로 실행 가능한가?
- [ ] 헬퍼 메소드로 중복을 제거했는가?
- [ ] AssertJ를 활용하여 가독성 높은 assertion을 작성했는가?
- [ ] 예외 테스트 시 에러 코드까지 검증하는가?
- [ ] 메소드명만 보고도 무엇을 테스트하는지 알 수 있는가?

---

## 참고 자료

- [AssertJ 공식 문서](https://assertj.github.io/doc/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
