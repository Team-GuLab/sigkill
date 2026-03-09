package com.gulab.sigkillserver.domain.room.repository;

import com.gulab.sigkillserver.domain.room.model.Room;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {

    Room save(Room room);

    Optional<Room> findById(String roomId);

    List<Room> findAll();

    /**
     * 전체 방 개수 조회
     *
     * @return 전체 방 개수
     */
    long count();

    void deleteById(String roomId);

    boolean existsById(String roomId);

    /**
     * 모든 방 데이터 정리
     *
     * @return 삭제된 방 수
     */
    int clear();
}
