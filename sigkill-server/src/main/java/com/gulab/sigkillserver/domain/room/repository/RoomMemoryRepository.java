package com.gulab.sigkillserver.domain.room.repository;

import com.gulab.sigkillserver.domain.room.model.Room;
import java.util.Comparator;
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
            throw new IllegalStateException("Room ID 가 이미 존재합니다.: " + room.getRoomId());
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
    public List<Room> findAll(Comparator<Room> comparator, int offset, int limit) {
        return store.values().stream()
                .sorted(comparator)
                .skip(offset)
                .limit(limit)
                .toList();
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
}
