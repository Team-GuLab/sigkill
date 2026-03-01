package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.domain.game.model.GamePlayer;
import java.util.List;

public interface GamePlayerRepository {
    GamePlayer save(GamePlayer gamePlayer);

    List<GamePlayer> getByGameId(long gameId);

    void deleteByGameId(long gameId);
}
