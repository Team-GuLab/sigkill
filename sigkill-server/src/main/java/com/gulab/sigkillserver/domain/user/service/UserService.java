package com.gulab.sigkillserver.domain.user.service;

import com.gulab.sigkillserver.domain.user.dto.response.LoginResponse;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import com.gulab.sigkillserver.domain.user.util.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * 비회원 로그인
     */
    public LoginResponse loginAsGuest() {
        String nickname = NicknameGenerator.generateRandomNickname();
        User user = User.builder()
                .userId(nickname) // TODO: 임시 userId, 추후 세션 아이디로 변경 필요
                .userName(nickname)
                .build();
        userRepository.save(user);
        log.info("비회원 로그인 처리 완료 - nickname: {}", nickname);
        return new LoginResponse(nickname);
    }
}
