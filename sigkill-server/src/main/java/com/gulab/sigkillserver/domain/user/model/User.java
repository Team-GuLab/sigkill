package com.gulab.sigkillserver.domain.user.model;

import lombok.Getter;

/**
 * 서비스 전체 사용자 정보
 */
@Getter
public class User {

    private final String id;
    private final String nickname;
    private final UserRole role;

    private User(String id, String nickname, UserRole userRole) {
        this.id = id;
        this.nickname = nickname;
        this.role = userRole;
    }

    public static User create(String sessionId, String nickname, UserRole userRole) {
        return new User(sessionId, nickname, userRole);
    }
}
