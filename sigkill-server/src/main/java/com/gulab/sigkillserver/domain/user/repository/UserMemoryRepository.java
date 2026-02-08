package com.gulab.sigkillserver.domain.user.repository;

import com.gulab.sigkillserver.domain.user.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class UserMemoryRepository implements UserRepository {

    private final Map<String, User> store = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        store.put(user.getUserId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(String userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String userId) {
        store.remove(userId);
    }

    @Override
    public boolean existsById(String userId) {
        return store.containsKey(userId);
    }

    @Override
    public void deleteAll() {
        store.clear();
    }

    /**
     * 테스트용 유틸리티 메서드 - 저장된 데이터 개수 확인
     */
    public int count() {
        return store.size();
    }
}
