package com.gulab.sigkillserver.domain.room.repository;

import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.user.model.User;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {

    Room save(Room room);

    Optional<Room> findById(String roomId);

    List<Room> findAll();

    void deleteById(String roomId);

    boolean existsById(String roomId);
}
