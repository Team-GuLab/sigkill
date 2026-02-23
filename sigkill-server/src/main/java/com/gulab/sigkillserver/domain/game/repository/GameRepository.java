package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.domain.game.model.Game;
import java.util.Optional;

public interface GameRepository {

    Game save(Game game);

    Optional<Game> findByRoomId(String roomId);

    void deleteByRoomId(String roomId);
}
