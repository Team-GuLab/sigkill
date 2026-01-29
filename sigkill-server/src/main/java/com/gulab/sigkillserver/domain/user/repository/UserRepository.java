package com.gulab.sigkillserver.domain.user.repository;

import com.gulab.sigkillserver.domain.user.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * User Redis Repository
 * 서비스 전체 사용자 정보 관리
 */
@Repository
public interface UserRepository extends CrudRepository<User, String> {}
