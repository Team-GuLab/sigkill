package com.gulab.sigkillserver.domain.room.repository;

import com.gulab.sigkillserver.domain.room.model.Room;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Room Repository
 */
@Repository
public interface RoomRepository {

    /**
     * 방 목록 조회 (페이지네이션)
     */
    List<Room> findAllWithPagination(int page, int size);

    /**
     * 방 ID로 조회
     */
    Optional<Room> findById(Long roomId);

    /**
     * 방 저장
     */
    Room save(Room room);

    /**
     * 전체 방 개수
     */
    long count();

    /**
     * 방 존재 여부 확인
     */
    boolean existsById(Long roomId);
}
