package com.gulab.sigkillserver.domain.room.repository;

import com.gulab.sigkillserver.domain.room.model.Player;
import java.util.List;
import java.util.Optional;

public interface PlayerRepository {

    Optional<Player> findById(Long userId);

    Player create(Player player);

    void deleteById(Long playerId);

    List<Player> findAll();

    /**
     * 방 안에 있는 플레이어 다 찾기
     */
    List<Player> findAllByRoomId(String roomId);

    /**
     * 방의 플레이어 수 조회
     */
    int countByRoomId(String roomId);

    /**
     * 특정 방에 특정 유저가 있는지 확인
     */
    boolean existsByRoomIdAndUserId(String roomId, Long userId);

    /**
     * 모든 플레이어 데이터 정리
     *
     * @return 삭제된 플레이어 수
     */
    int clear();
}
