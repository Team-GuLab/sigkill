package com.gulab.sigkillserver.domain.room.repository;

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
        return store.put(room.getId(), room);
    }

    @Override
    public Optional<Room> findById(String roomId) {
        return Optional.ofNullable(store.get(roomId));
    }

    @Override
    public List<Room> findAll() {
        return store.values().stream().toList();
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
