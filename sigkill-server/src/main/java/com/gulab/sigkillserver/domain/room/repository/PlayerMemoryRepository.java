package com.gulab.sigkillserver.domain.room.repository;

import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.PLAYER_ID_ALREADY_EXISTS;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.PLAYER_NOT_FOUND;

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
            throw new CustomException(PLAYER_ID_ALREADY_EXISTS);
        }
        return player;
    }

    @Override
    public void deleteById(Long userId) {
        Player removed = store.remove(userId);
        if (removed == null) {
            throw new CustomException(PLAYER_NOT_FOUND);
        }
    }

    @Override
    public List<Player> findAllByRoomId(String roomId) {
        return store.values().stream()
                .filter(p -> p.getRoomId().equals(roomId))
                .toList();
    }
}
