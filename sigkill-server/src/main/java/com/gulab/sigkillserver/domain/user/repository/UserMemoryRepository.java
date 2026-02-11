package com.gulab.sigkillserver.domain.user.repository;

import com.gulab.sigkillserver.domain.user.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class UserMemoryRepository implements UserRepository {

    private final Map<Long, User> store = new ConcurrentHashMap<>();

    private final Map<String, Long> sessionIndex = new ConcurrentHashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public User save(User user) {
        if (user.getUserId() == null) {
            long newId = idGenerator.incrementAndGet();
            user = user.withUserId(newId);
        }
        store.put(user.getUserId(), user);

        if (user.getSessionId() != null) {
            sessionIndex.put(user.getSessionId(), user.getUserId());
        }

        return user;
    }

    @Override
    public Optional<User> findById(Long userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override public Optional<User> findBySessionId(String sessionId) {
        Long useId = sessionIndex.get(sessionId);
        if (useId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(useId));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(store.values());
    }
}
