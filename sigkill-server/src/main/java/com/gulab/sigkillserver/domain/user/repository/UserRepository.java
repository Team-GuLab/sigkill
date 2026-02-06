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
    Optional<User> findById(String userId);

    /**
     * 모든 사용자 조회
     */
    List<User> findAll();

    /**
     * ID로 사용자 삭제
     */
    void deleteById(String userId);

    /**
     * 사용자 존재 여부 확인
     */
    boolean existsById(String userId);

    /**
     * 모든 사용자 삭제
     */
    void deleteAll();
}
