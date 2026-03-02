package com.gulab.sigkillserver.domain.game.repository;

import com.gulab.sigkillserver.domain.game.model.Game;
import java.util.Optional;

public interface GameRepository {

    Game save(Game game);

    Optional<Game> findById(Long gameId);

    void deleteById(Long gameId);

    Optional<Game> findByRoomId(String roomId);

    void deleteByRoomId(String roomId);

    /**
     * 모든 게임 데이터 정리
     *
     * @return 삭제된 게임 수
     */
    int clear();
}
