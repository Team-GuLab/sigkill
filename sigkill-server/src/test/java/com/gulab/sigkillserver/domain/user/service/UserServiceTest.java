package com.gulab.sigkillserver.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gulab.sigkillserver.domain.user.dto.response.LoginResponse;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.model.UserRole;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import com.gulab.sigkillserver.domain.user.repository.UserMemoryRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@DisplayName("UserService 테스트")
class UserServiceTest {

    private UserService userService;
    private UserRepository userRepository;
    private HttpSession mockSession;

    // 테스트용 고정 데이터
    private static final String TEST_SESSION_ID = "test-session-123";
    private static final String TEST_NICKNAME = "테스트닉네임";
    private static final UserRole TEST_ROLE = UserRole.GUEST;

    @BeforeEach
    void setup() {
        userRepository = new UserMemoryRepository();
        userService = new UserService(userRepository);
        mockSession = mock(HttpSession.class);
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("비회원 로그인 기능")
    class LoginAsGuestTests {

        @Test
        @DisplayName("새로운 세션으로 로그인 시 사용자가 생성된다")
        void loginAsGuest_NewUser_Success() {
            // Given
            when(mockSession.getId()).thenReturn(TEST_SESSION_ID);

            // When
            LoginResponse response = userService.loginAsGuest(mockSession);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.nickname()).isNotBlank();

            // 사용자가 실제로 저장되었는지 확인
            Optional<User> savedUser = userRepository.findById(TEST_SESSION_ID);
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getSessionId()).isEqualTo(TEST_SESSION_ID);
            assertThat(savedUser.get().getRole()).isEqualTo(UserRole.GUEST);
        }

        @Test
        @DisplayName("기존 세션으로 로그인 시 기존 사용자 정보를 반환한다")
        void loginAsGuest_ExistingUser_Success() {
            // Given
            User existingUser = createAndSaveUser(TEST_SESSION_ID, TEST_NICKNAME);
            when(mockSession.getId()).thenReturn(TEST_SESSION_ID);

            // When
            LoginResponse response = userService.loginAsGuest(mockSession);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.nickname()).isEqualTo(TEST_NICKNAME);

            // 사용자가 중복 생성되지 않았는지 확인
            assertThat(userRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("로그인 후 SecurityContext가 올바르게 설정된다")
        void loginAsGuest_SecurityContextIsSet() {
            // Given
            when(mockSession.getId()).thenReturn(TEST_SESSION_ID);

            // When
            userService.loginAsGuest(mockSession);

            // Then
            SecurityContext securityContext = SecurityContextHolder.getContext();
            assertThat(securityContext).isNotNull();

            Authentication authentication = securityContext.getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isEqualTo(TEST_SESSION_ID);
            assertThat(authentication.getAuthorities())
                    .extracting("authority")
                    .contains(UserRole.GUEST.getKey());
        }

        @Test
        @DisplayName("로그인 후 세션에 SecurityContext가 저장된다")
        void loginAsGuest_SessionContainsSecurityContext() {
            // Given
            when(mockSession.getId()).thenReturn(TEST_SESSION_ID);

            // When
            userService.loginAsGuest(mockSession);

            // Then
            verify(mockSession, times(1)).setAttribute(
                    eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY),
                    any(SecurityContext.class)
            );
        }

        @Test
        @DisplayName("생성된 닉네임은 공백을 포함한다")
        void loginAsGuest_NicknameContainsSpace() {
            // Given
            when(mockSession.getId()).thenReturn(TEST_SESSION_ID);

            // When
            LoginResponse response = userService.loginAsGuest(mockSession);

            // Then
            assertThat(response.nickname()).contains(" ");
        }

        @Test
        @DisplayName("생성된 사용자의 역할은 GUEST이다")
        void loginAsGuest_UserRoleIsGuest() {
            // Given
            when(mockSession.getId()).thenReturn(TEST_SESSION_ID);

            // When
            userService.loginAsGuest(mockSession);

            // Then
            User savedUser = userRepository.findById(TEST_SESSION_ID).orElseThrow();
            assertThat(savedUser.getRole()).isEqualTo(UserRole.GUEST);
            assertThat(savedUser.getRole().getKey()).isEqualTo("ROLE_GUEST");
        }

        @Test
        @DisplayName("다른 세션으로 로그인 시 각각 다른 사용자가 생성된다")
        void loginAsGuest_DifferentSessions_CreateDifferentUsers() {
            // Given
            String sessionId1 = "session-1";
            String sessionId2 = "session-2";
            HttpSession mockSession1 = mock(HttpSession.class);
            HttpSession mockSession2 = mock(HttpSession.class);
            when(mockSession1.getId()).thenReturn(sessionId1);
            when(mockSession2.getId()).thenReturn(sessionId2);

            // When
            LoginResponse response1 = userService.loginAsGuest(mockSession1);
            SecurityContextHolder.clearContext(); // 컨텍스트 초기화
            LoginResponse response2 = userService.loginAsGuest(mockSession2);

            // Then
            assertThat(userRepository.findAll()).hasSize(2);
            assertThat(userRepository.findById(sessionId1)).isPresent();
            assertThat(userRepository.findById(sessionId2)).isPresent();
        }

        @Test
        @DisplayName("기존 사용자로 재로그인 시 닉네임이 변경되지 않는다")
        void loginAsGuest_ExistingUser_NicknameUnchanged() {
            // Given
            String originalNickname = "원래닉네임";
            User existingUser = createAndSaveUser(TEST_SESSION_ID, originalNickname);
            when(mockSession.getId()).thenReturn(TEST_SESSION_ID);

            // When
            LoginResponse response = userService.loginAsGuest(mockSession);

            // Then
            assertThat(response.nickname()).isEqualTo(originalNickname);
            User user = userRepository.findById(TEST_SESSION_ID).orElseThrow();
            assertThat(user.getNickname()).isEqualTo(originalNickname);
        }
    }

    @Nested
    @DisplayName("닉네임 생성 규칙")
    class NicknameGenerationTests {

        @Test
        @DisplayName("여러 사용자 생성 시 각각 다른 닉네임이 생성된다")
        void loginAsGuest_MultipleTimes_GeneratesDifferentNicknames() {
            // Given & When
            LoginResponse response1 = createGuestUserWithSession("session-1");
            LoginResponse response2 = createGuestUserWithSession("session-2");
            LoginResponse response3 = createGuestUserWithSession("session-3");
            LoginResponse response4 = createGuestUserWithSession("session-4");
            LoginResponse response5 = createGuestUserWithSession("session-5");

            // Then
            // 최소 2개 이상은 달라야 함 (확률적으로 모두 다를 가능성이 높음)
            long distinctCount = java.util.stream.Stream.of(
                    response1.nickname(),
                    response2.nickname(),
                    response3.nickname(),
                    response4.nickname(),
                    response5.nickname()
            ).distinct().count();

            assertThat(distinctCount).isGreaterThanOrEqualTo(2);
        }
    }

    // 헬퍼 메소드

    /**
     * User 객체를 생성하고 저장
     */
    private User createAndSaveUser(String sessionId, String nickname) {
        User user = User.create(sessionId, nickname, TEST_ROLE);
        return userRepository.save(user);
    }

    /**
     * 특정 세션 ID로 게스트 로그인 수행
     */
    private LoginResponse createGuestUserWithSession(String sessionId) {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn(sessionId);
        SecurityContextHolder.clearContext();
        return userService.loginAsGuest(session);
    }
}
