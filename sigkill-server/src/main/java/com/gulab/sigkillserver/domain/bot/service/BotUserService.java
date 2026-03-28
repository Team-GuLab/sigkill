package com.gulab.sigkillserver.domain.bot.service;

import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.model.UserRole;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import com.gulab.sigkillserver.domain.user.util.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotUserService {

    private static final String BOT_NICKNAME_PREFIX = "[봇] ";

    private final UserRepository userRepository;

    public User createBotUser() {
        String nickname = BOT_NICKNAME_PREFIX + NicknameGenerator.generateRandomNickname();
        return userRepository.save(User.create(null, nickname, UserRole.BOT));
    }

    public void deleteBotUser(Long userId) {
        userRepository.deleteById(userId);
    }

    public boolean isBotUser(Long userId) {
        return userRepository.findById(userId)
                .map(User::getRole)
                .map(role -> role == UserRole.BOT)
                .orElse(false);
    }
}
