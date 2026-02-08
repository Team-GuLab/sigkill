package com.gulab.sigkillserver.domain.user.model;

import lombok.Getter;

/**
 * 서비스 전체 사용자 정보
 */
@Getter
public class User {

    private final String userId;
    private final String nickname;
    private final UserRole role;

    private User(String userId, String nickname, UserRole userRole) {
        this.userId = userId;
        this.nickname = nickname;
        this.role = userRole;
    }

    public static User create(String userId, String nickname, UserRole userRole) {
        return new User(userId, nickname, userRole);
    }
}
