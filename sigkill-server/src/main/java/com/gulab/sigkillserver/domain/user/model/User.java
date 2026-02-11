package com.gulab.sigkillserver.domain.user.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

/**
 * 서비스 전체 사용자 정보
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @With
    private Long userId;
    private String sessionId;
    private String nickname;
    private UserRole role;

    private User(Long userId, String sessionId, String nickname, UserRole userRole) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.nickname = nickname;
        this.role = userRole;
    }

    public static User create(String sessionId, String nickname, UserRole userRole) {
        return new User(null, sessionId, nickname, userRole);
    }
}
