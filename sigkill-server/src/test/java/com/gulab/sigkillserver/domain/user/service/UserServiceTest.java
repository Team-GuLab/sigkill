package com.gulab.sigkillserver.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gulab.sigkillserver.domain.user.dto.rest.response.LoginResponse;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.model.UserRole;
import com.gulab.sigkillserver.domain.user.repository.UserMemoryRepository;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

class UserServiceTest {

    private static final String TEST_SESSION_ID = "test-session-123";
    private static final String TEST_NICKNAME = "테스트닉네임";
    private static final UserRole TEST_ROLE = UserRole.GUEST;
    private UserService userService;
    private UserRepository userRepository;
    private HttpSession mockSession;

    @BeforeEach
    void setup() {
        userRepository = new UserMemoryRepository();
        userService = new UserService(userRepository);
        mockSession = mock(HttpSession.class);
        SecurityContextHolder.clearContext();
    }

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

    @Nested
    class 비회원_로그인_기능 {

        @Test
        void 새로운_세션으로_로그인_시_사용자가_생성된다() {
            // Given
            when(mockSession.getId()).thenReturn(TEST_SESSION_ID);

            // When
            LoginResponse response = userService.loginAsGuest(mockSession);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.nickname()).isNotBlank();

            Optional<User> savedUser = userRepository.findById(TEST_SESSION_ID);
            assertThat(savedUser).isPresent();
            assertThat(savedUser.get().getId()).isEqualTo(TEST_SESSION_ID);
            assertThat(savedUser.get().getRole()).isEqualTo(UserRole.GUEST);
        }

        @Test
        void 기존_세션으로_로그인_시_기존_사용자_정보를_반환한다() {
            // Given
            createAndSaveUser(TEST_SESSION_ID, TEST_NICKNAME);
            when(mockSession.getId()).thenReturn(TEST_SESSION_ID);

            // When
            LoginResponse response = userService.loginAsGuest(mockSession);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.nickname()).isEqualTo(TEST_NICKNAME);
            assertThat(userRepository.findAll()).hasSize(1);
        }

        @Test
        void 로그인_후_SecurityContext가_올바르게_설정된다() {
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
        void 로그인_후_세션에_SecurityContext가_저장된다() {
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
        void 생성된_닉네임은_공백을_포함한다() {
            // Given
            when(mockSession.getId()).thenReturn(TEST_SESSION_ID);

            // When
            LoginResponse response = userService.loginAsGuest(mockSession);

            // Then
            assertThat(response.nickname()).contains(" ");
        }

        @Test
        void 생성된_사용자의_역할은_GUEST이다() {
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
        void 다른_세션으로_로그인_시_각각_다른_사용자가_생성된다() {
            // Given
            String sessionId1 = "session-1";
            String sessionId2 = "session-2";
            HttpSession mockSession1 = mock(HttpSession.class);
            HttpSession mockSession2 = mock(HttpSession.class);
            when(mockSession1.getId()).thenReturn(sessionId1);
            when(mockSession2.getId()).thenReturn(sessionId2);

            // When
            userService.loginAsGuest(mockSession1);
            SecurityContextHolder.clearContext();
            userService.loginAsGuest(mockSession2);

            // Then
            assertThat(userRepository.findAll()).hasSize(2);
            assertThat(userRepository.findById(sessionId1)).isPresent();
            assertThat(userRepository.findById(sessionId2)).isPresent();
        }

        @Test
        void 기존_사용자로_재로그인_시_닉네임이_변경되지_않는다() {
            // Given
            String originalNickname = "원래닉네임";
            createAndSaveUser(TEST_SESSION_ID, originalNickname);
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
    class 닉네임_생성_규칙 {

        @Test
        void 여러_사용자_생성_시_각각_다른_닉네임이_생성된다() {
            // Given & When
            LoginResponse response1 = createGuestUserWithSession("session-1");
            LoginResponse response2 = createGuestUserWithSession("session-2");
            LoginResponse response3 = createGuestUserWithSession("session-3");
            LoginResponse response4 = createGuestUserWithSession("session-4");
            LoginResponse response5 = createGuestUserWithSession("session-5");

            // Then
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
}
