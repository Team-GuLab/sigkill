package com.gulab.sigkillserver.domain.user.service;

import com.gulab.sigkillserver.domain.user.dto.rest.response.LoginResponse;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.model.UserRole;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import com.gulab.sigkillserver.domain.user.util.NicknameGenerator;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * 비회원 로그인
     */
    public LoginResponse loginAsGuest(HttpSession session) {
        // 세션 ID를 userId로 사용
        String sessionId = session.getId();

        User user = userRepository.findBySessionId(sessionId)
                .orElseGet(() -> createUser(sessionId));

        // Spring Security 인증 정보 설정
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getUserId(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getKey()))
                );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // 세션에 SecurityContext 저장
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );

        log.info("비회원 로그인 처리 완료 - sessionId: {}, userName: {}", sessionId, user.getNickname());
        return new LoginResponse(user.getNickname());
    }

    private User createUser(String sessionId) {
        String nickname = NicknameGenerator.generateRandomNickname();
        User newUser = User.create(sessionId, nickname, UserRole.GUEST);
        newUser = userRepository.save(newUser);
        log.info("새 비회원 사용자 생성 - id: {} sessionId: {}, nickname: {}, role: {}",
                newUser.getUserId(), sessionId, nickname, UserRole.GUEST.getKey());
        return newUser;
    }
}
