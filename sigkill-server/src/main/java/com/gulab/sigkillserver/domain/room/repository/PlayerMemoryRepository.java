package com.gulab.sigkillserver.domain.room.repository;

import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.USER_ALREADY_IN_ROOM;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.room.model.Player;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class PlayerMemoryRepository implements PlayerRepository {

    private final Map<Long, Player> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Player> findById(Long userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public Player create(Player player) {
        Player existing = store.putIfAbsent(player.getUserId(), player);
        if (existing != null) {
            throw new CustomException(USER_ALREADY_IN_ROOM);
        }
        return player;
    }

    @Override
    public void deleteById(Long userId) {
        store.remove(userId);
    }

    @Override public List<Player> findAll() {
        return store.values().stream().toList();
    }

    @Override
    public List<Player> findAllByRoomId(String roomId) {
        return store.values().stream()
                .filter(p -> p.getRoomId().equals(roomId))
                .toList();
    }

    @Override
    public int countByRoomId(String roomId) {
        return (int) store.values().stream()
                .filter(p -> p.getRoomId().equals(roomId))
                .count();
    }

    @Override
    public boolean existsByRoomIdAndUserId(String roomId, Long userId) {
        Player player = store.get(userId);
        return player != null && player.getRoomId().equals(roomId);
    }

    @Override
    public int clear() {
        int removedCount = store.size();
        store.clear();
        return removedCount;
    }
}
