package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.exception.GameErrorCode;
import com.gulab.sigkillserver.domain.game.model.Game;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class GameMemoryRepository implements GameRepository {

    private final Map<String, Game> store = new ConcurrentHashMap<>();

    @Override
    public Game save(Game game) {
        Game existing = store.putIfAbsent(game.getRoomId(), game);
        if (existing != null) {
            throw new CustomException(GameErrorCode.GAME_ALREADY_EXISTS);
        }
        return game;
    }

    @Override
    public Optional<Game> findByRoomId(String roomId) {
        return Optional.ofNullable(store.get(roomId));
    }

    @Override
    public void deleteByRoomId(String roomId) {
        store.remove(roomId);
    }
}
