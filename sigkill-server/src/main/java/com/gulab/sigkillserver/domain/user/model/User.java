package com.gulab.sigkillserver.domain.user.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

/**
 * 서비스 전체 사용자 정보
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "user", timeToLive = 86400) // 24시간 TTL
public class User {

    @Id
    private String userId;

    private String userName;

    private Role role;

    @Builder
    public User(String userId, String userName, Role role) {
        this.userId = userId;
        this.userName = userName;
        this.role = role != null ? role : Role.GUEST;
    }
}
