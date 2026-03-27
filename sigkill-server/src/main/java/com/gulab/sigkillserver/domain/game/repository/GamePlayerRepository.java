package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.domain.game.model.GamePlayer;
import java.util.List;

public interface GamePlayerRepository {
    GamePlayer save(GamePlayer gamePlayer);

    List<GamePlayer> getByGameId(long gameId);

    void deleteByGameIdAndUserId(long gameId, long userId);

    void deleteByGameId(long gameId);

    /**
     * 모든 게임 플레이어 데이터 정리
     *
     * @return 삭제된 게임 플레이어 수
     */
    int clear();
}
