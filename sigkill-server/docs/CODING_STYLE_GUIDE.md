# Java/Spring Boot 프로젝트 코딩 스타일 가이드

> 이 문서는 Spring Boot 기반 Java 프로젝트의 코딩 스타일과 구조적 패턴을 정의합니다.
> 도메인 특화 비즈니스 로직과 무관하게 어떤 프로젝트에도 적용 가능합니다.

---

## 목차
1. [프로젝트 구조 및 패키지 구성](#1-프로젝트-구조-및-패키지-구성)
2. [클래스 네이밍 컨벤션](#2-클래스-네이밍-컨벤션)
3. [메서드 네이밍 패턴](#3-메서드-네이밍-패턴)
4. [어노테이션 사용 패턴](#4-어노테이션-사용-패턴)
5. [DTO/Response/Request 객체 설계](#5-dtoresponserequest-객체-설계)
6. [예외 처리 패턴](#6-예외-처리-패턴)
7. [빌더 패턴 및 생성자 패턴](#7-빌더-패턴-및-생성자-패턴)
8. [주석 및 문서화 스타일](#8-주석-및-문서화-스타일)
9. [코드 포맷팅](#9-코드-포맷팅)
10. [Validation 및 검증 방식](#10-validation-및-검증-방식)
11. [핵심 원칙 요약](#11-핵심-원칙-요약)

---

## 1. 프로젝트 구조 및 패키지 구성

### 계층적 디렉토리 구조

```
com.company.project/
├── config/                     # 설정 관련
│   ├── constant/              # 상수 정의
│   ├── application/           # 애플리케이션 설정 (Async, Swagger, etc.)
│   └── infrastructure/        # 인프라 설정 (Security, Redis, S3, etc.)
│       └── security/
├── common/                     # 공통 기능
│   ├── BaseEntity.java        # 엔티티 기본 클래스
│   ├── BaseResponse.java      # 응답 기본 형식
│   ├── security/              # 보안 관련 공통
│   └── exception/             # 예외 처리
│       ├── handler/           # 전역 예외 핸들러
│       └── global/
└── domain/                     # 도메인별 로직
    ├── user/
    │   ├── controller/        # HTTP 엔드포인트
    │   ├── service/           # 비즈니스 로직
    │   ├── repository/        # 데이터 접근
    │   ├── entity/            # JPA 엔티티
    │   ├── dto/               # 데이터 전송 객체
    │   │   ├── request/
    │   │   ├── response/
    │   │   └── (일반 DTO)
    │   └── exception/         # 도메인 특화 예외
    ├── order/
    └── payment/
```

### 핵심 특징

- **Clean Architecture** 기반 계층 분리
- **도메인 독립성**: 각 도메인이 controller, service, repository, entity, dto, exception을 모두 포함
- **설정 중앙화**: `config` 디렉토리에서 모든 설정 관리
- **공통 기능**: `common` 디렉토리에서 전역 기능 처리

---

## 2. 클래스 네이밍 컨벤션

### Entity 클래스
- **형식**: 단수형 명사
- **예시**: `Member`, `Order`, `Product`, `InviteCode`
- **어노테이션**: `@Entity`
- **상속**: `extends BaseEntity` (생성시간/수정시간 자동 관리)

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {
    // ...
}
```

### Controller 클래스
- **형식**: `{Domain}Controller`
- **예시**: `MemberController`, `OrderController`, `ProductController`
- **어노테이션**: `@RestController`, `@RequestMapping("/api")`

```java
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class MemberController {
    // ...
}
```

### Service 클래스
- **형식**: `{Domain}Service`
- **예시**: `MemberService`, `OrderService`, `ProductService`
- **어노테이션**: `@Service`, `@Transactional`

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemberService {
    // ...
}
```

### Repository 인터페이스
- **형식**: `{Domain}Repository`
- **예시**: `MemberRepository`, `OrderRepository`
- **상속**: `extends JpaRepository<Entity, ID>`

```java
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
}
```

### DTO 클래스
- **Request**: `{명사}Request` (예: `MemberAddRequest`, `OrderCreateRequest`)
- **Response**: `{명사}Response` (예: `MemberResponse`, `OrderResponse`)
- **일반 DTO**: `{명사}DTO` (예: `MemberDTO`, `OrderDTO`)

```java
public record MemberAddRequest(String name, String email) { }
public record MemberResponse(Long id, String name, String email) { }
public record MemberDTO(Long id, String name) { }
```

### 예외 클래스
- **ErrorCode**: `{Domain}CustomErrorCode` (Enum)
- **예외**: `CustomException` 단일 클래스 사용

```java
@Getter
@AllArgsConstructor
public enum MemberCustomErrorCode implements CustomErrorCodeInterface {
    MEMBER_NOT_FOUND("MEMBER001", "회원을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    MEMBER_ALREADY_EXISTS("MEMBER005", "이미 존재하는 회원입니다", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
```

---

## 3. 메서드 네이밍 패턴

### 조회 메서드

| 패턴 | 용도 | 반환 타입 | 예시 |
|------|------|----------|------|
| `fetch{Entity}()` | 컬렉션 조회 | `List<DTO>` | `fetchOrders()`, `fetchMembers()` |
| `get{Entity}()` | 단건 조회 | `DTO` | `getMember()`, `getOrder()` |
| `findBy{Property}()` | Repository 조회 | `Optional<Entity>` | `findByEmail()`, `findById()` |

```java
// Service
public List<OrderDTO> fetchOrders(Long memberId) { }
public MemberDTO getMember(Long id) { }

// Repository
Optional<Member> findByEmail(String email);
```

### 생성 메서드

| 패턴 | 용도 | 예시 |
|------|------|------|
| `create{Entity}()` | 엔티티 생성 | `createOrder()`, `createMember()` |
| `add{Entity}()` | 자식 엔티티 추가 | `addOrderItem()`, `addComment()` |
| `save{Entity}()` | 저장 | `saveMember()` |

```java
public OrderResponse createOrder(OrderCreateRequest request) { }
public void addOrderItem(Long orderId, OrderItemRequest item) { }
```

### 삭제 메서드

| 패턴 | 용도 | 예시 |
|------|------|------|
| `delete{Entity}()` | 삭제 | `deleteMember()`, `deleteOrder()` |
| `remove{Entity}()` | 제거 | `removeOrderItem()` |
| `clear{Relationships}()` | 관계 정리 | `clearAssociations()` |

```java
public void deleteMember(Long id) { }
public void removeOrderItem(Long orderId, Long itemId) { }
```

### 유효성 검사 메서드

| 패턴 | 용도 | 반환 타입 | 예시 |
|------|------|----------|------|
| `validate{Condition}()` | 검증(예외 발생) | `void` | `validateMemberExists()` |
| `exists{Condition}()` | 존재 여부 | `boolean` | `existsByEmail()` |
| `is{Condition}()` | 상태 확인 | `boolean` | `isExpired()` |

```java
private void validateMemberExists(Long id) {
    if (!memberRepository.existsById(id)) {
        throw new CustomException(MemberCustomErrorCode.MEMBER_NOT_FOUND);
    }
}
```

### 변환 메서드

| 패턴 | 용도 | 예시 |
|------|------|------|
| `convertTo{Type}()` | 엔티티/DTO 변환 | `convertToDTO()`, `convertToResponse()` |
| `of()` | 정적 팩토리 메서드 | `MemberDTO.of(entity)` |

```java
// Service
private MemberDTO convertToDTO(Member member) { }

// DTO Record
public record MemberDTO(...) {
    public static MemberDTO of(Member member) {
        return new MemberDTO(...);
    }
}
```

---

## 4. 어노테이션 사용 패턴

### 클래스 레벨 어노테이션

```java
// Entity
@Entity
@Table(name = "members", indexes = {
    @Index(name = "idx_email", columnList = "email")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity { }

// Controller
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "1. 회원 API", description = "회원 관련 API")
public class MemberController { }

// Service
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemberService { }

// Configuration
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig { }
```

### 필드 레벨 어노테이션

```java
// JPA 관련
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "member_id")
private Long id;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "order_id")
private Order order;

@OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
private List<Order> orders;

@Enumerated(EnumType.STRING)
private Role role;

// 검증
@NotBlank
@Email
@Column(nullable = false, unique = true)
private String email;

@Size(min = 2, max = 12)
private String name;

// 감사(Auditing)
@CreatedDate
private LocalDateTime createdAt;

@LastModifiedDate
private LocalDateTime updatedAt;
```

### 메서드 레벨 어노테이션

```java
@Operation(summary = "회원 조회", description = "ID로 회원을 조회합니다")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "404", description = "회원 없음", content = @Content)
})
@GetMapping("/members/{id}")
public BaseResponse<MemberDTO> getMember(
    @Parameter(description = "회원 ID", required = true, example = "1")
    @PathVariable Long id) {
    // ...
}

@Transactional
@PostMapping("/members")
public BaseResponse<MemberDTO> createMember(
    @RequestBody MemberAddRequest request) {
    // ...
}
```

### 어노테이션 정렬 순서

1. **클래스 레벨**:
   - `@Entity/@RestController/@Service/@Configuration`
   - `@Table/@RequestMapping`
   - `@RequiredArgsConstructor/@AllArgsConstructor`
   - `@Getter/@Setter`
   - `@Slf4j`

2. **메서드 레벨**:
   - `@Operation`
   - `@ApiResponses`
   - `@Transactional`
   - `@PostMapping/@GetMapping/@PutMapping/@DeleteMapping`

3. **파라미터**:
   - `@Parameter/@Schema`
   - `@RequestBody/@PathVariable/@RequestParam`
   - `@AuthenticationPrincipal`

---

## 5. DTO/Response/Request 객체 설계

### Java Record 사용 원칙

모든 DTO는 **Java Record**로 작성하여 불변성과 간결성을 보장합니다.

```java
// Request DTO
public record MemberAddRequest(
    @Schema(description = "회원 이름", example = "홍길동")
    String name,

    @Schema(description = "이메일", example = "hong@example.com")
    @Email
    String email
) { }

// Response DTO
public record MemberResponse(
    Long id,
    String name,
    String email,
    LocalDateTime createdAt
) { }

// 일반 DTO (정적 팩토리 메서드 포함)
public record MemberDTO(
    @Schema(description = "회원 ID", example = "1")
    Long id,

    @Schema(description = "이름", example = "홍길동")
    String name,

    @Schema(description = "이메일", example = "hong@example.com")
    String email
) {
    // Entity -> DTO 변환
    public static MemberDTO of(Member member) {
        return new MemberDTO(
            member.getId(),
            member.getName(),
            member.getEmail()
        );
    }
}
```

### DTO 디렉토리 구조

```
domain/member/dto/
├── MemberDTO.java              # 일반 DTO
├── request/
│   ├── MemberAddRequest.java
│   └── MemberUpdateRequest.java
└── response/
    ├── MemberResponse.java
    └── MemberIdResponse.java
```

### 중첩 DTO 사용

```java
public record OrderResponse(
    Long id,
    LocalDateTime orderDate,
    MemberDTO member,              // 중첩 DTO
    List<OrderItemDTO> items,      // 리스트 중첩 DTO
    AddressDTO shippingAddress     // 중첩 DTO
) { }
```

### 핵심 특징

- **불변성**: Java Record로 자동 보장
- **간결성**: getter, constructor, equals, hashCode 자동 생성
- **문서화**: `@Schema` 어노테이션으로 Swagger 문서 자동화
- **변환 로직**: `of()` 정적 팩토리 메서드 사용
- **로직 최소화**: Record에 비즈니스 로직 포함하지 않음

---

## 6. 예외 처리 패턴

### 커스텀 예외 체계

```java
// 1. 기본 예외 클래스
@Getter
@AllArgsConstructor
public class CustomException extends RuntimeException {
    private final CustomErrorCodeInterface errorCode;

    public CustomErrorCode getErrorCode() {
        return this.errorCode.getErrorCode();
    }
}

// 2. 에러코드 인터페이스
public interface CustomErrorCodeInterface {
    CustomErrorCode getErrorCode();
}

// 3. 에러코드 Enum (도메인별)
@Getter
@AllArgsConstructor
public enum MemberCustomErrorCode implements CustomErrorCodeInterface {
    MEMBER_NOT_FOUND("MEMBER001", "회원을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    MEMBER_ALREADY_EXISTS("MEMBER002", "이미 존재하는 회원입니다", HttpStatus.CONFLICT),
    INVALID_NAME_LENGTH("MEMBER003", "이름은 2자에서 12자 이내로 입력해주세요", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public CustomErrorCode getErrorCode() {
        return CustomErrorCode.builder()
                .code(code)
                .message(message)
                .httpStatus(httpStatus)
                .build();
    }
}

// 4. 에러코드 데이터 클래스
@Getter
@Builder
public class CustomErrorCode {
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}

// 5. 전역 예외 핸들러
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<String>> handleCustomException(CustomException e) {
        log.error("CustomException 발생: {}", e.getErrorCode().getMessage(), e);
        return createResponseEntity(e.getErrorCode());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<String>> handleConstraintViolationException(
            ConstraintViolationException e) {
        log.error("ConstraintViolationException 발생", e);
        return createResponseEntity(
            GlobalCustomErrorCode.VALIDATION_FAILED.getErrorCode()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<String>> handleException(Exception e) {
        log.error("예상치 못한 예외 발생", e);
        return createResponseEntity(
            GlobalCustomErrorCode.INTERNAL_SERVER_ERROR.getErrorCode()
        );
    }

    private ResponseEntity<BaseResponse<String>> createResponseEntity(CustomErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus().value())
                .body(BaseResponse.onFailure(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        null
                ));
    }
}
```

### 예외 사용 패턴

```java
// Service 내에서 예외 발생
public MemberDTO getMember(Long id) {
    Member member = memberRepository.findById(id)
            .orElseThrow(() -> new CustomException(MemberCustomErrorCode.MEMBER_NOT_FOUND));
    return MemberDTO.of(member);
}

// 검증 실패 시
private void validateNameLength(String name) {
    if (name.length() < 2 || name.length() > 12) {
        throw new CustomException(MemberCustomErrorCode.INVALID_NAME_LENGTH);
    }
}

// 중복 검증
private void validateDuplicateEmail(String email) {
    if (memberRepository.findByEmail(email).isPresent()) {
        throw new CustomException(MemberCustomErrorCode.MEMBER_ALREADY_EXISTS);
    }
}
```

### 핵심 특징

- **단일 예외 클래스**: `CustomException` 하나로 모든 비즈니스 예외 처리
- **타입 안정성**: Enum 기반 에러코드 관리
- **도메인별 분리**: `MemberCustomErrorCode`, `OrderCustomErrorCode` 등
- **일괄 처리**: 전역 예외 핸들러에서 통합 응답 생성
- **로깅**: 모든 예외 자동 로깅

---

## 7. 빌더 패턴 및 생성자 패턴

### Entity 생성 패턴

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    // 1. 빌더 패턴 (유연한 객체 생성)
    @Builder
    public Member(String name, String email, Role role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // 2. 커스텀 생성자 (특정 시나리오용)
    public Member(String name, String email) {
        this.name = name;
        this.email = email;
        this.role = Role.USER;  // 기본값 설정
    }
}

// 사용
Member member1 = Member.builder()
        .name("홍길동")
        .email("hong@example.com")
        .role(Role.ADMIN)
        .build();

Member member2 = new Member("김철수", "kim@example.com");
```

### 초기화 시점 계산 값

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InviteCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public InviteCode() {
        this.code = generateRandomCode();
        this.expiresAt = LocalDateTime.now().plusHours(1);  // 1시간 후 만료
    }

    private String generateRandomCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
```

### 핵심 원칙

- **`@Builder`**: 필드가 많거나 유연한 초기화가 필요한 경우
- **`@NoArgsConstructor(access = AccessLevel.PROTECTED)`**: JPA용 기본 생성자 보호
- **커스텀 생성자**: 필수 필드만 초기화하는 경우
- **초기화 로직**: 생성 시점에 계산된 값은 생성자에서 할당

---

## 8. 주석 및 문서화 스타일

### JavaDoc 주석 패턴

```java
/**
 * 이메일로 회원을 조회합니다.
 */
private MemberDTO findMemberByEmail(String email) {
    return memberRepository.findByEmail(email)
            .map(MemberDTO::of)
            .orElseThrow(() -> new CustomException(MemberCustomErrorCode.MEMBER_NOT_FOUND));
}

/**
 * Member 엔티티를 DTO로 변환합니다.
 */
private MemberDTO convertToDTO(Member member) {
    return MemberDTO.of(member);
}

/**
 * 회원 존재 여부를 검증합니다.
 */
private void validateMemberExists(Long id) {
    if (!memberRepository.existsById(id)) {
        throw new CustomException(MemberCustomErrorCode.MEMBER_NOT_FOUND);
    }
}
```

### 인라인 주석

```java
public void addMemberToOrder(Long orderId, Long memberId) {
    Order order = findOrderById(orderId);
    Member member = findMemberById(memberId);

    // 연관관계 편의 메서드 호출
    order.addMember(member);

    // 변경 감지(Dirty Checking)로 자동 업데이트
}
```

### Swagger 문서화

```java
@Operation(
    summary = "회원 정보 저장",
    description = "회원 정보를 저장합니다. 인증된 사용자 정보를 바탕으로 저장합니다."
)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200",
        description = "저장 성공"
    ),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 (이름 길이 제한, 중복 회원 등)",
        content = @Content
    ),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패",
        content = @Content
    )
})
@PostMapping("/members")
public BaseResponse<MemberDTO> createMember(
    @Parameter(description = "저장할 회원 정보", required = true)
    @RequestBody MemberAddRequest request,

    @Parameter(description = "현재 인증된 사용자 정보", hidden = true)
    @AuthenticationPrincipal JwtUserDetails userDetails) {
    // ...
}
```

### 주석 원칙

- **간결성**: 메서드명이 명확하면 주석 생략 가능
- **1줄 JavaDoc**: 메서드 시작 부분에 간단한 설명
- **복잡한 로직**: 인라인 주석으로 보충 설명
- **Swagger**: API 문서화는 `@Operation`, `@ApiResponse` 활용

---

## 9. 코드 포맷팅

### 들여쓰기

- **4개 공백** 사용 (탭 아님)
- 각 중첩 레벨마다 4칸 증가

```java
public class Example {
    public void method() {
        if (condition) {
            for (int i = 0; i < 10; i++) {
                doSomething();
            }
        }
    }
}
```

### 줄바꿈 규칙

```java
// 1. 긴 메서드 체인은 개행
Member member = memberRepository.findById(id)
        .orElseThrow(() -> new CustomException(MemberCustomErrorCode.MEMBER_NOT_FOUND));

List<MemberDTO> members = memberRepository.findAll().stream()
        .filter(m -> m.getRole() == Role.USER)
        .map(MemberDTO::of)
        .collect(Collectors.toList());

// 2. 메서드 파라미터 분리 (3개 이상인 경우)
public BaseResponse<MemberDTO> createMember(
        @Parameter(description = "회원 정보", required = true)
        @RequestBody MemberAddRequest request,

        @Parameter(description = "인증 정보", hidden = true)
        @AuthenticationPrincipal JwtUserDetails userDetails) {
    // ...
}

// 3. 어노테이션과 메서드 분리
@PostMapping("/members")
@Operation(summary = "회원 생성")
@ApiResponses(value = { ... })
public BaseResponse<MemberDTO> createMember(...) {
    // ...
}

// 4. 빌더 패턴 개행
Member member = Member.builder()
        .name("홍길동")
        .email("hong@example.com")
        .role(Role.USER)
        .build();
```

### 클래스 구조

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {
    // 필드 선언 (공행 없음)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // 생성자 (1줄 공행)
    @Builder
    public Member(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // 비즈니스 메서드 (1줄 공행)
    public void updateName(String name) {
        this.name = name;
    }
}
```

### 임포트

- **정렬**: IDE 자동 정렬 사용
- **와일드카드**: 사용하지 않음
- **정적 임포트**: 상수나 enum은 정적 임포트 허용

```java
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;
import static com.company.project.domain.member.entity.Role.*;
```

---

## 10. Validation 및 검증 방식

### Entity 레벨 검증

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @NotBlank
    @Size(min = 2, max = 12)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$")
    private String phoneNumber;
}
```

### Service 레벨 검증

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    public MemberDTO createMember(String name, String email) {
        // 1. Null/Blank 검증
        if (name == null || name.isBlank()) {
            throw new CustomException(MemberCustomErrorCode.MISSING_PARAMETER);
        }

        // 2. 값 정규화
        name = name.trim();
        email = email.toLowerCase();

        // 3. 길이 검증
        validateNameLength(name);

        // 4. 중복 검증
        validateDuplicateEmail(email);

        // 5. 비즈니스 로직 실행
        Member member = Member.builder()
                .name(name)
                .email(email)
                .build();

        return MemberDTO.of(memberRepository.save(member));
    }

    private void validateNameLength(String name) {
        if (name.length() < 2 || name.length() > 12) {
            throw new CustomException(MemberCustomErrorCode.INVALID_NAME_LENGTH);
        }
    }

    private void validateDuplicateEmail(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new CustomException(MemberCustomErrorCode.MEMBER_ALREADY_EXISTS);
        }
    }

    private void validatePageSize(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new CustomException(GlobalCustomErrorCode.INVALID_PAGE_SIZE);
        }
    }
}
```

### Request DTO 레벨 문서화

```java
public record MemberAddRequest(
    @Schema(description = "회원 이름", example = "홍길동")
    @NotBlank
    @Size(min = 2, max = 12)
    String name,

    @Schema(description = "이메일", example = "hong@example.com")
    @NotBlank
    @Email
    String email
) { }
```

### 검증 체인

1. **Entity 레벨**: `@NotBlank`, `@Email`, `@Size` 등 기본 검증
2. **Service 레벨**: 비즈니스 로직 검증 (중복, 상태, 권한 등)
3. **전역 예외 핸들러**: 검증 실패 시 일괄 처리

### 핵심 원칙

- **Entity**: 기본 검증만 선언적으로 정의
- **Service**: 복잡한 비즈니스 검증은 명시적으로 구현
- **예외**: 모든 검증 실패는 `CustomException` 발생
- **문서화**: `@Schema`로 API 문서 자동화

---

## 11. 핵심 원칙 요약

### 일관성 (Consistency)
- 모든 도메인이 동일한 구조와 네이밍 규칙 준수
- Controller → Service → Repository → Entity 계층 철저히 분리
- DTO는 request/response/일반으로 명확히 구분

### 명확성 (Clarity)
- 메서드명만으로 기능 파악 가능
- `fetch*`, `get*`, `create*`, `validate*` 등 일관된 동사 사용
- 어노테이션으로 의도 명확화 (`@Transactional`, `@Operation`)

### 모듈화 (Modularity)
- 각 도메인이 완전히 독립적
- 공통 기능은 `common` 패키지로 중앙화
- 설정은 `config` 패키지로 분리

### 불변성 (Immutability)
- Java Record로 DTO 불변성 보장
- Entity 필드는 `final` 또는 setter 없이 관리
- 생성자/빌더로만 초기화

### 타입 안정성 (Type Safety)
- Enum 기반 에러코드 관리
- Generic 활용 (`JpaRepository<Entity, ID>`)
- Optional 사용으로 null 처리 명확화

### 로깅 (Logging)
- `@Slf4j`로 모든 Controller/Service 로깅
- 예외 발생 시 자동 로깅
- 중요 비즈니스 로직 실행 시 로그 기록

### 문서화 (Documentation)
- Swagger `@Operation`, `@ApiResponse`로 API 자동 문서화
- `@Schema`로 DTO 필드 설명
- JavaDoc은 간결하게 1줄로 작성

### 예외 처리 (Exception Handling)
- 단일 `CustomException` 클래스 사용
- Enum 기반 도메인별 에러코드 정의
- 전역 예외 핸들러에서 일괄 처리

### 검증 (Validation)
- Entity: 선언적 검증 (`@NotBlank`, `@Email`)
- Service: 비즈니스 검증 (중복, 상태, 권한)
- 3단계 검증 체인 (Entity → Service → Handler)

### 생성자 패턴 (Constructor Pattern)
- `@Builder`로 유연한 객체 생성
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` JPA용
- 필요 시 커스텀 생성자 추가

---

## 적용 가이드

이 스타일 가이드를 새 프로젝트에 적용하려면:

1. **패키지 구조** 먼저 설정 (`domain`, `common`, `config`)
2. **BaseEntity, BaseResponse** 공통 클래스 작성
3. **예외 처리 체계** 구축 (CustomException, ErrorCode, Handler)
4. **첫 도메인** 완성 후 다른 도메인에 패턴 복제
5. **Swagger 설정** 및 문서화 자동화
6. **테스트 코드**도 동일한 네이밍 패턴 적용

---

**버전**: 1.0
**최종 수정일**: 2026-01-29
**적용 대상**: Java 21+, Spring Boot 3.x
