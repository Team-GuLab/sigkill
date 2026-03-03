package com.gulab.sigkillserver.domain.test.controller;

import com.gulab.sigkillserver.common.BaseResponse;
import com.gulab.sigkillserver.domain.game.repository.GamePlayerRepository;
import com.gulab.sigkillserver.domain.game.repository.GameRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizChoiceNumberMappingRepository;
import com.gulab.sigkillserver.domain.game.repository.SelectedChoiceRepository;
import com.gulab.sigkillserver.domain.game.service.GameFlowOrchestrator;
import com.gulab.sigkillserver.domain.room.dto.shared.RoomInfoResponse;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import com.gulab.sigkillserver.domain.user.dto.rest.response.LoginResponse;
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
    private static final int ROOM_A_OCCUPANCY = 5;
    private static final int ROOM_B_OCCUPANCY = 6;
    private static final int ROOM_C_OCCUPANCY = 2;
    private static final int ROOM_D_OCCUPANCY = 3;

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final RoomRepository roomRepository;
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final SelectedChoiceRepository selectedChoiceRepository;
    private final QuizChoiceNumberMappingRepository quizChoiceNumberMappingRepository;
    private final GameFlowOrchestrator gameFlowOrchestrator;
    private final RoomService roomService;

    /**
     * 임의 유저 20명 생성 + 5/6, 6/6, 2/6, 3/6 상태의 방 4개 생성 - 테스트 방 A(5/6): 참가자 전원 READY - 테스트 방 B/C: 참가자 전원 UNREADY - 테스트 방
     * D(3/6): INGAME 상태
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
                cursor,
                true,
                false
        );
        cursor += ROOM_A_OCCUPANCY;

        SeededRoom roomB = createRoomWithOccupancy(
                "테스트 방 B",
                ROOM_CAPACITY,
                ROOM_B_OCCUPANCY,
                createdUsers,
                cursor,
                false,
                false
        );
        cursor += ROOM_B_OCCUPANCY;

        SeededRoom roomC = createRoomWithOccupancy(
                "테스트 방 C",
                ROOM_CAPACITY,
                ROOM_C_OCCUPANCY,
                createdUsers,
                cursor,
                false,
                false
        );
        cursor += ROOM_C_OCCUPANCY;

        SeededRoom roomD = createRoomWithOccupancy(
                "테스트 방 D",
                ROOM_CAPACITY,
                ROOM_D_OCCUPANCY,
                createdUsers,
                cursor,
                false,
                true
        );

        log.info("테스트 데이터 생성 완료 - users: {}, rooms: [{}, {}, {}, {}]",
                createdUsers.size(), roomA.roomId(), roomB.roomId(), roomC.roomId(), roomD.roomId());

        return BaseResponse.onSuccess(new TestSeedResponse(
                createdUsers.size(),
                List.of(roomA, roomB, roomC, roomD)
        ));
    }

    /**
     * 유저/방/게임 관련 인메모리 데이터를 전체 초기화한다.
     */
    @PostMapping("/clear-memory")
    public BaseResponse<MemoryCleanupResponse> clearMemory() {
        GameFlowOrchestrator.FlowCleanupResult flowCleanupResult = gameFlowOrchestrator.clearAllFlows();
        int clearedSelectedChoiceCount = selectedChoiceRepository.clear();
        int clearedQuizChoiceMappingCount = quizChoiceNumberMappingRepository.clear();
        int clearedGamePlayerCount = gamePlayerRepository.clear();
        int clearedGameCount = gameRepository.clear();
        int clearedPlayerCount = playerRepository.clear();
        int clearedRoomCount = roomRepository.clear();
        int clearedUserCount = userRepository.clear();

        MemoryCleanupResponse response = new MemoryCleanupResponse(
                clearedUserCount,
                clearedRoomCount,
                clearedPlayerCount,
                clearedGameCount,
                clearedGamePlayerCount,
                clearedSelectedChoiceCount,
                clearedQuizChoiceMappingCount,
                flowCleanupResult.canceledScheduledTaskCount(),
                flowCleanupResult.clearedInitialQuizStartFlagCount()
        );

        log.info("테스트 메모리 정리 완료 - users={}, rooms={}, players={}, games={}, gamePlayers={}, selectedChoices={}, "
                        + "quizChoiceMappings={}, canceledFlows={}, clearedInitialFlags={}",
                response.clearedUserCount(),
                response.clearedRoomCount(),
                response.clearedPlayerCount(),
                response.clearedGameCount(),
                response.clearedGamePlayerCount(),
                response.clearedSelectedChoiceCount(),
                response.clearedQuizChoiceMappingCount(),
                response.canceledScheduledFlowCount(),
                response.clearedInitialQuizStartFlagCount());

        return BaseResponse.onSuccess(response);
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
            int startIndex,
            boolean allReady,
            boolean inGame
    ) {
        if (targetOccupancy < 1 || targetOccupancy > capacity) {
            throw new IllegalArgumentException("targetOccupancy must be between 1 and capacity");
        }
        if (startIndex + targetOccupancy > users.size()) {
            throw new IllegalArgumentException("Not enough users to seed requested occupancy");
        }

        LoginResponse host = users.get(startIndex);
        RoomInfoResponse roomInfoResponse = roomService.createRoom(roomTitle, capacity, host.userId());
        String roomId = roomInfoResponse.roomId();

        for (int i = 1; i < targetOccupancy; i++) {
            LoginResponse guest = users.get(startIndex + i);
            roomService.joinRoom(roomId, guest.userId());
        }
        if (allReady) {
            playerRepository.findAllByRoomId(roomId).forEach(Player::ready);
        }
        if (inGame) {
            roomRepository.findById(roomId).ifPresent(room -> {
                room.startGame();
                log.info("테스트 방 INGAME 설정 - roomId: {}", roomId);
            });
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

    public record MemoryCleanupResponse(
            int clearedUserCount,
            int clearedRoomCount,
            int clearedPlayerCount,
            int clearedGameCount,
            int clearedGamePlayerCount,
            int clearedSelectedChoiceCount,
            int clearedQuizChoiceMappingCount,
            int canceledScheduledFlowCount,
            int clearedInitialQuizStartFlagCount
    ) {
    }
}
