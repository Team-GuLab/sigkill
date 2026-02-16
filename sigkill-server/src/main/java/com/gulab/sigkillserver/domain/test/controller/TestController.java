package com.gulab.sigkillserver.domain.test.controller;

import com.gulab.sigkillserver.common.BaseResponse;
import com.gulab.sigkillserver.domain.user.dto.rest.response.LoginResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomCreateResponse;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.model.UserRole;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트 데이터 생성을 위한 Controller
 */
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private static final int TEST_USER_COUNT = 20;
    private static final int ROOM_CAPACITY = 6;
    private static final int ROOM_A_OCCUPANCY = 4;
    private static final int ROOM_B_OCCUPANCY = 6;
    private static final int ROOM_C_OCCUPANCY = 2;

    private final UserRepository userRepository;
    private final RoomService roomService;

    /**
     * 임의 유저 20명 생성 + 4/6, 6/6, 2/6 상태의 방 3개 생성
     */
    @PostMapping("/seed-rooms")
    public BaseResponse<TestSeedResponse> seedRooms() {
        List<LoginResponse> createdUsers = createRandomUsers(TEST_USER_COUNT);

        int cursor = 0;

        SeededRoom roomA = createRoomWithOccupancy(
                "테스트 방 A",
                ROOM_CAPACITY,
                ROOM_A_OCCUPANCY,
                createdUsers,
                cursor
        );
        cursor += ROOM_A_OCCUPANCY;

        SeededRoom roomB = createRoomWithOccupancy(
                "테스트 방 B",
                ROOM_CAPACITY,
                ROOM_B_OCCUPANCY,
                createdUsers,
                cursor
        );
        cursor += ROOM_B_OCCUPANCY;

        SeededRoom roomC = createRoomWithOccupancy(
                "테스트 방 C",
                ROOM_CAPACITY,
                ROOM_C_OCCUPANCY,
                createdUsers,
                cursor
        );

        log.info("테스트 데이터 생성 완료 - users: {}, rooms: [{}, {}, {}]",
                createdUsers.size(), roomA.roomId(), roomB.roomId(), roomC.roomId());

        return BaseResponse.onSuccess(new TestSeedResponse(
                createdUsers.size(),
                List.of(roomA, roomB, roomC)
        ));
    }

    private List<LoginResponse> createRandomUsers(int count) {
        List<LoginResponse> users = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            String randomKey = UUID.randomUUID().toString().substring(0, 8);
            User createdUser = userRepository.save(
                    User.create("test-session-" + randomKey + "-" + i, "테스트유저-" + randomKey + "-" + i, UserRole.GUEST)
            );
            users.add(new LoginResponse(createdUser.getUserId(), createdUser.getNickname()));
        }
        return users;
    }

    private SeededRoom createRoomWithOccupancy(
            String roomTitle,
            int capacity,
            int targetOccupancy,
            List<LoginResponse> users,
            int startIndex
    ) {
        if (targetOccupancy < 1 || targetOccupancy > capacity) {
            throw new IllegalArgumentException("targetOccupancy must be between 1 and capacity");
        }
        if (startIndex + targetOccupancy > users.size()) {
            throw new IllegalArgumentException("Not enough users to seed requested occupancy");
        }

        LoginResponse host = users.get(startIndex);
        RoomCreateResponse roomCreateResponse = roomService.createRoom(roomTitle, capacity, host.userId());
        String roomId = roomCreateResponse.roomId();

        for (int i = 1; i < targetOccupancy; i++) {
            LoginResponse guest = users.get(startIndex + i);
            roomService.joinRoom(roomId, guest.userId());
        }

        return new SeededRoom(roomId, roomTitle, targetOccupancy, capacity);
    }

    public record TestSeedResponse(
            int createdUserCount,
            List<SeededRoom> rooms
    ) {
    }

    public record SeededRoom(
            String roomId,
            String roomTitle,
            int playerCount,
            int capacity
    ) {
    }
}
