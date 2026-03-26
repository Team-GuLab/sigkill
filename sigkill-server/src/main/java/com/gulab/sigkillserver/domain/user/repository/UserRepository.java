package com.gulab.sigkillserver.domain.user.repository;

import com.gulab.sigkillserver.domain.user.model.User;
import java.util.List;
import java.util.Optional;

/**
 * User Repository 인터페이스 도메인 레이어 - Spring에 독립적인 순수 인터페이스 서비스 전체 사용자 정보 관리
 */
public interface UserRepository {

    /**
     * 사용자 저장
     */
    User save(User user);

    /**
     * ID로 사용자 조회
     */
    Optional<User> findById(Long userId);

    /**
     * Session ID로 사용자 조회
     */
    Optional<User> findBySessionId(String sessionId);

    /**
     * ID로 사용자 삭제
     */
    void deleteById(Long userId);

    /**
     * 모든 사용자 조회
     */
    List<User> findAll();

    /**
     * 모든 사용자 데이터 정리
     *
     * @return 삭제된 사용자 수
     */
    int clear();
}
