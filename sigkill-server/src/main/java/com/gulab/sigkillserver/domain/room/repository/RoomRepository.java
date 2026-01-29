package com.gulab.sigkillserver.domain.room.repository;

import com.gulab.sigkillserver.domain.room.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Room Redis Repository
 * 방 정보 관리
 */
@Repository
public interface RoomRepository extends CrudRepository<Room, Long> {

    /**
     * 특정 상태의 방 목록 조회
     */
    Page<Room> findAllByStatus(Pageable pageable, Room.RoomStatus status);
}
