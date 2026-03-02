package com.gulab.sigkillserver.domain.lock;

import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_ROOM_NUMBER;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MIN_ROOM_NUMBER;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class RoomLockManager {

    private static final int STRIPE_SIZE = MAX_ROOM_NUMBER - MIN_ROOM_NUMBER + 1;
    private final ReentrantLock[] roomLocks = initializeLocks();

    public <T> T executeWithLock(String roomId, Supplier<T> action) {
        ReentrantLock lock = acquire(roomId);
        try {
            return action.get();
        } finally {
            release(lock);
        }
    }

    int lockCount() {
        return roomLocks.length;
    }

    private ReentrantLock acquire(String roomId) {
        Objects.requireNonNull(roomId, "roomId must not be null");
        ReentrantLock lock = roomLocks[toLockIndex(roomId)];
        lock.lock();
        return lock;
    }

    private void release(ReentrantLock lock) {
        lock.unlock();
    }

    private ReentrantLock[] initializeLocks() {
        ReentrantLock[] locks = new ReentrantLock[STRIPE_SIZE];
        for (int i = 0; i < STRIPE_SIZE; i++) {
            locks[i] = new ReentrantLock();
        }
        return locks;
    }

    private int toLockIndex(String roomId) {
        int roomNumber;
        try {
            roomNumber = Integer.parseInt(roomId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("roomId must be a 4-digit integer", e);
        }
        if (roomNumber < MIN_ROOM_NUMBER || roomNumber > MAX_ROOM_NUMBER) {
            throw new IllegalArgumentException("roomId out of valid range: " + roomId);
        }
        return roomNumber - MIN_ROOM_NUMBER;
    }
}
