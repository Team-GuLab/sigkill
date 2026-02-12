package com.gulab.sigkillserver.domain.room.repository;

import com.gulab.sigkillserver.domain.room.model.Player;
import java.util.List;
import java.util.Optional;

public interface PlayerRepository {

    Optional<Player> findById(Long userId);

    Player create(Player player);

    void deleteById(Long playerId);

    /**
     * 방 안에 있는 플레이어 다 찾기
     */
    List<Player> findAllByRoomId(String roomId);
}
