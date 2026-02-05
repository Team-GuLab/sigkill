package com.gulab.sigkillserver.domain.user.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 권한
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {
    GUEST("ROLE_GUEST", "게스트"),
    USER("ROLE_USER", "일반 사용자"),
    ADMIN("ROLE_ADMIN", "관리자");

    private final String key;
    private final String title;
}
