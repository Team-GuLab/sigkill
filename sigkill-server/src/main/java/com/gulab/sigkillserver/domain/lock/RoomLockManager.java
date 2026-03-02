package com.gulab.sigkillserver.domain.lock;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class RoomLockManager {

    private final ConcurrentMap<String, ReentrantLock> roomLocks = new ConcurrentHashMap<>();

    public <T> T executeWithLock(String roomId, Supplier<T> action) {
        ReentrantLock lock = acquire(roomId);
        try {
            return action.get();
        } finally {
            release(roomId, lock);
        }
    }

    int lockCount() {
        return roomLocks.size();
    }

    private ReentrantLock acquire(String roomId) {
        Objects.requireNonNull(roomId, "roomId must not be null");
        ReentrantLock lock = roomLocks.computeIfAbsent(roomId, ignored -> new ReentrantLock());
        lock.lock();
        return lock;
    }

    private void release(String roomId, ReentrantLock lock) {
        lock.unlock();
    }
}
