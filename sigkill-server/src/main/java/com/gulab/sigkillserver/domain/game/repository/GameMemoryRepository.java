package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.exception.GameErrorCode;
import com.gulab.sigkillserver.domain.game.model.Game;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class GameMemoryRepository implements GameRepository {

    private final Map<Long, Game> store = new ConcurrentHashMap<>();

    private final Map<String, Long> roomIdIndex = new ConcurrentHashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public synchronized Game save(Game game) {
        if (game.getGameId() == null) {
            long newId = idGenerator.incrementAndGet();
            game = game.withGameId(newId);
        }

        if (game.getRoomId() != null) {
            Long existingGameId = roomIdIndex.putIfAbsent(game.getRoomId(), game.getGameId());
            if (existingGameId != null && !existingGameId.equals(game.getGameId())) {
                throw new CustomException(GameErrorCode.GAME_ALREADY_EXISTS);
            }
        }

        store.put(game.getGameId(), game);
        return game;
    }

    @Override
    public Optional<Game> findById(Long gameId) {
        return Optional.ofNullable(store.get(gameId));
    }

    @Override
    public synchronized void deleteById(Long gameId) {
        Game removedGame = store.remove(gameId);
        if (removedGame != null && removedGame.getRoomId() != null) {
            roomIdIndex.remove(removedGame.getRoomId());
        }
    }

    @Override
    public Optional<Game> findByRoomId(String roomId) {
        Long gameId = roomIdIndex.get(roomId);
        if (gameId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(gameId));
    }

    @Override
    public synchronized void deleteByRoomId(String roomId) {
        Long gameId = roomIdIndex.remove(roomId);
        if (gameId != null) {
            store.remove(gameId);
        }
    }
}
