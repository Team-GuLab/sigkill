package com.gulab.sigkillserver.domain.room.repository;

import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.user.model.User;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {

    Room save(Room room);

    Optional<Room> findById(String roomId);

    List<Room> findAll();

    /**
     * 정렬 및 페이징된 방 목록 조회
     *
     * @param comparator 정렬 기준
     * @param offset 시작 위치
     * @param limit 조회 개수
     * @return 정렬 및 페이징된 방 목록
     */
    List<Room> findAll(Comparator<Room> comparator, int offset, int limit);

    /**
     * 전체 방 개수 조회
     *
     * @return 전체 방 개수
     */
    long count();

    void deleteById(String roomId);

    boolean existsById(String roomId);
}
