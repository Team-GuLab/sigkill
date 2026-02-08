# [GUIDE] Spring Boot STOMP 웹소켓 구현 표준

본 가이드는 Java 21 및 Spring Boot 3.5 환경에서 **세션 기반(Cookie/Session)** 인증을 사용하는 STOMP 웹소켓 구현의 표준을 정의한다.

## 1. 환경 및 기술 스택

* **Language:** Java 21 (`record` 기능 필수 사용)
* **Framework:** Spring Boot 3.5+
* **Dependency:** `spring-boot-starter-websocket`, `spring-boot-starter-security`
* **Security Model:** **Stateful Session (JSESSIONID)**
    * *참고: JWT 토큰 파싱 방식이 아님. 브라우저 쿠키를 통한 자동 인증 연동을 따름.*

## 2. 아키텍처 원칙

### 2.1 엔드포인트 전략 (Endpoint Strategy)

* REST API와 웹소켓 연결 엔드포인트를 명확히 분리한다.
* **WebSocket Endpoint:** `/ws` (Root 레벨에 위치)
* **REST API Endpoint:** `/api/v1/**`
* **이유:** 웹소켓은 지속 연결이므로 리소스 버전닝(`/v1`)보다 프로토콜 분리가 우선됨.

### 2.2 패키지 구조 (Package Structure)

REST와 WS 관련 컴포넌트를 물리적으로 분리하여 관리한다.

```text
src/main/java/com/example/project
 ├── controller
 │    ├── rest         // @RestController (HTTP)
 │    └── ws           // @Controller (STOMP)
 └── dto
      ├── rest         // REST API용 DTO
      │    ├── request
      │    └── response
      └── ws           // WebSocket용 DTO
           ├── request  // Inbound (Client -> Server)
           └── response // Outbound (Server -> Clients)
```

## 3. 구현 규칙 (Implementation Rules)

### 3.1 DTO 패턴 (Java Records)

- 모든 DTO는 record를 사용하여 불변(Immutable) 객체로 구현한다.
- Request/Response 분리 원칙:
    - Request DTO: 클라이언트에서 서버로 보내는 데이터. 절대 `senderId` 등 식별 정보를 포함하지 않는다. (보안 위조 방지)
    - Response DTO: 서버가 클라이언트로 보내는 데이터. `sender` 정보, 타임스탬프, 메시지 ID 등 상세 정보를 모두 포함한다.

### 3.2 보안 및 인증 (Security)

- 인증 방식: Spring Security가 핸드셰이크 시점에 주입한 `Principal` 객체를 활용한다.
- 검증 시점: `StompCommand.CONNECT` 프레임 수신 시점에만 검증한다.
- 금지 사항: 인터셉터에서 헤더(Token)를 수동으로 파싱하거나 DB를 조회하지 않는다. (`accessor.getUser()`만 신뢰)

## 4. 코드 템플릿 (Code Patterns)

### 4.1 WebSocket Config

```Java
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompHandler stompHandler;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 엔드포인트: /ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompHandler); // 핸들러 등록
    }
}
```

### 4.2 Security Interceptor

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // CONNECT 시점에만 세션 검증
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            Principal principal = accessor.getUser();

            // 세션이 만료되었거나 인증되지 않은 경우 차단
            if (principal == null || !(principal instanceof Authentication)) {
                throw new AccessDeniedException("로그인이 필요합니다.");
            }
            // 통과 시 로그 등 작성 가능
        }
        return message;
    }
}
```

### 4.3 Controller & DTO Definition

```java
// Request: 작성자 정보 없음 (Principal에서 추출)
public record ChatMessageRequest(String roomId, String content) {}

// Response: 상세 정보 포함
public record ChatMessageResponse(String id, String content, UserInfo sender) {}

// Controller Usage
@MessageMapping("/chat/send")
@SendTo("/topic/messages")
public ChatMessageResponse sendMessage(ChatMessageRequest request, Principal principal) {
    // principal.getName()을 사용하여 실제 작성자 식별
    return chatService.process(request, principal.getName());
}
```