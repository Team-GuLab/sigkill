package com.gulab.sigkillserver.domain.room.repository;

import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_ID_ALREADY_EXISTS;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.room.model.Room;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class RoomMemoryRepository implements RoomRepository {

    private final Map<String, Room> store = new ConcurrentHashMap<>();

    @Override
    public Room save(Room room) {
        Room existing = store.putIfAbsent(room.getRoomId(), room);
        if (existing != null) {
            throw new CustomException(ROOM_ID_ALREADY_EXISTS);
        }
        return room;
    }

    @Override
    public Optional<Room> findById(String roomId) {
        return Optional.ofNullable(store.get(roomId));
    }

    @Override
    public List<Room> findAll() {
        return new java.util.ArrayList<>(store.values());
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void deleteById(String roomId) {
        store.remove(roomId);
    }

    @Override
    public boolean existsById(String roomId) {
        return store.containsKey(roomId);
    }

    @Override
    public int clear() {
        int removedCount = store.size();
        store.clear();
        return removedCount;
    }
}
