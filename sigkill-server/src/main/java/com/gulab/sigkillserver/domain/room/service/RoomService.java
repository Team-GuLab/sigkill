package com.gulab.sigkillserver.domain.room.service;

import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.DEFAULT_CAPACITY;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_CAPACITY;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_ROOM_NUMBER;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_TITLE_LENGTH;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MIN_CAPACITY;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MIN_PLAYERS_TO_START;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MIN_ROOM_NUMBER;
import static com.gulab.sigkillserver.domain.room.exception.PlayerErrorCode.PLAYER_NOT_IN_ANY_ROOM;
import static com.gulab.sigkillserver.domain.room.exception.PlayerErrorCode.PLAYER_NOT_IN_ROOM;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.HOST_CANNOT_READY;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.NOT_ENOUGH_PLAYERS_TO_START;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ONLY_HOST_CAN_START_GAME;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.PLAYERS_NOT_READY;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_CAPACITY_INVALID;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_CREATE_ERROR;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_FULL;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_ID_ALREADY_EXISTS;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_IN_GAME;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_NOT_FOUND;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_NUMBER_ERROR;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_PAGING_PARAMETER_INVALID;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_TITLE_INVALID;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.USER_ALREADY_IN_ROOM;
import static com.gulab.sigkillserver.domain.user.exception.UserErrorCode.USER_NOT_FOUND;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.dto.stomp.event.GameStartEvent;
import com.gulab.sigkillserver.domain.game.service.GameService;
import com.gulab.sigkillserver.domain.lock.RoomLockManager;
import com.gulab.sigkillserver.domain.room.dto.rest.response.LeaveRoomResult;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomListResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomResponse;
import com.gulab.sigkillserver.domain.room.dto.shared.PlayerInfo;
import com.gulab.sigkillserver.domain.room.dto.shared.RoomInfoResponse;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.HostChangedEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerJoinEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerLeftEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerReadyEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerUnreadyEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.RoomSnapshotEvent;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Room Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private static final String HOST_CHANGED_REASON_HOST_LEFT = "HOST_LEFT";
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;

    private final RoomLockManager roomLockManager;
    private final GameService gameService;

    /**
     * 방 목록 조회
     */
    public RoomListResponse fetchRooms(int page, int size) {
        validatePaginationParameters(page, size);

        Comparator<Room> comparator = Comparator.comparing(this::canJoinRoom).reversed()
                .thenComparing(Room::getCreatedAt, Comparator.reverseOrder());
        // TODO: updatedAt 기준 정렬로 변경

        List<Room> sortedRooms = roomRepository.findAll().stream()
                .sorted(comparator)
                .toList();

        int totalCount = sortedRooms.size();
        int totalPages = (totalCount + size - 1) / size;
        int offset = page * size;

        // 페이지 범위 초과 시 빈 리스트 반환
        if (offset >= totalCount) {
            return new RoomListResponse(
                    Collections.emptyList(),
                    page,
                    size,
                    totalCount,
                    totalPages,
                    false
            );
        }

        Map<String, Long> playerCountMap = playerRepository.findAll().stream()
                .collect(Collectors.groupingBy(Player::getRoomId, Collectors.counting()));

        List<RoomResponse> roomResponses = sortedRooms.stream()
                .skip(offset)
                .limit(size)
                .map(room -> {
                    int playerCount = playerCountMap.getOrDefault(room.getRoomId(), 0L).intValue();
                    return RoomResponse.of(room, playerCount);
                })
                .toList();

        boolean hasNext = (offset + size) < totalCount;

        return new RoomListResponse(
                roomResponses,
                page,
                size,
                totalCount,
                totalPages,
                hasNext
        );
    }

    private boolean isRoomFull(Room room) {
        return getPlayerCountInRoom(room.getRoomId()) >= room.getCapacity();
    }

    private int getPlayerCountInRoom(String roomId) {
        return playerRepository.countByRoomId(roomId);
    }

    private boolean canJoinRoom(Room room) {
        return !room.isInGame() && !isRoomFull(room);
    }

    private void validatePaginationParameters(int page, int size) {
        if (page < 0) {
            throw new CustomException(ROOM_PAGING_PARAMETER_INVALID);
        }
        if (size <= 0 || size > 100) {
            throw new CustomException(ROOM_PAGING_PARAMETER_INVALID);
        }
    }

    /**
     * 방 생성
     */
    public synchronized RoomInfoResponse createRoom(String roomTitle, Integer capacity, Long userId) {
        roomTitle = roomTitle.strip();
        int resolvedCapacity = capacity != null ? capacity : DEFAULT_CAPACITY;
        validateRoomCreateRequest(roomTitle, resolvedCapacity);

        User host = getUserOrThrow(userId);
        validateUserNotInRoom(userId);

        int maxAttempts = 100;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                String roomId = generateRoomId();

                Room room = Room.create(roomId, roomTitle, userId, resolvedCapacity);
                room = roomRepository.save(room);

                Player hostPlayer = Player.create(userId, roomId, host.getNickname());
                playerRepository.create(hostPlayer);

                log.info("방 생성됨 - action=room-created, roomId={}, hostId={}, capacity={}",
                        roomId, userId, resolvedCapacity);
                return RoomInfoResponse.of(room);
            } catch (CustomException e) {
                if (ROOM_ID_ALREADY_EXISTS.name().equals(e.getErrorCode().getCode())) {
                    log.debug("Room ID 중복 발생, 재시도 중 (attempt: {}): {}", i + 1, e.getMessage());
                    continue;
                }
                throw e;
            }
        }

        throw new CustomException(ROOM_CREATE_ERROR);
    }

    /**
     * 4자리 랜덤 정수 생성 (1000 ~ 9999)
     */
    private String generateRoomId() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(MIN_ROOM_NUMBER, MAX_ROOM_NUMBER + 1));
    }

    private void validateRoomCreateRequest(String roomTitle, int capacity) {
        if (roomTitle.isBlank() || roomTitle.length() > MAX_TITLE_LENGTH) {
            throw new CustomException(ROOM_TITLE_INVALID);
        }

        if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
            throw new CustomException(ROOM_CAPACITY_INVALID);
        }
    }

    private void validateUserNotInRoom(Long userId) {
        if (playerRepository.findById(userId).isPresent()) {
            throw new CustomException(USER_ALREADY_IN_ROOM);
        }
    }

    private void validateRoomId(String roomId) {
        int roomIdInt;
        try {
            roomIdInt = Integer.parseInt(roomId);
        } catch (NumberFormatException e) {
            throw new CustomException(ROOM_NUMBER_ERROR);
        }
        if (roomIdInt < MIN_ROOM_NUMBER || roomIdInt > MAX_ROOM_NUMBER) {
            throw new CustomException(ROOM_NUMBER_ERROR);
        }
    }

    /**
     * 플레이어 방 참가
     */
    public RoomInfoResponse joinRoom(String roomId, Long userId) {
        validateRoomId(roomId);
        User user = getUserOrThrow(userId);

        RoomInfoResponse roomInfoResponse = roomLockManager.executeWithLock(roomId, () -> {
            Room room = getRoomOrThrow(roomId);
            validateCanJoinRoom(userId, room);
            Player player = Player.create(userId, roomId, user.getNickname());
            playerRepository.create(player);
            return RoomInfoResponse.of(room);
        });

        log.info("방 참가됨 - action=room-joined, roomId={}, userId={}",
                roomId, userId);
        return roomInfoResponse;
    }

    /**
     * 플레이어 방 참가 확정
     */
    public Optional<PlayerJoinEvent> confirmJoin(String roomId, Long userId) {
        validateRoomId(roomId);
        Optional<PlayerJoinEvent> playerJoinEvent = roomLockManager.executeWithLock(roomId, () -> {
            Room room = getRoomOrThrow(roomId);
            Player player = getPlayerInRoomOrThrow(userId, roomId);
            if (player.isActive()) {
                return Optional.empty();
            }
            player.activate();
            return Optional.of(PlayerJoinEvent.of(room, PlayerInfo.of(player, room.getHostId())));
        });

        log.info("방 참가 확정됨 - action=room-join-confirmed, roomId={}, userId={}, broadcasted={}",
                roomId, userId, playerJoinEvent.isPresent());
        return playerJoinEvent;
    }

    public boolean expirePendingJoin(String roomId, Long userId) {
        validateRoomId(roomId);
        return roomLockManager.executeWithLock(roomId, () -> {
            Player player = playerRepository.findById(userId).orElse(null);
            if (player == null || !roomId.equals(player.getRoomId()) || player.isActive()) {
                return false;
            }
            leaveRoom(roomId, userId);
            return true;
        });
    }

    private void validateCanJoinRoom(Long userId, Room room) {
        if (room.isInGame()) {
            throw new CustomException(ROOM_IN_GAME);
        }
        if (isRoomFull(room)) {
            throw new CustomException(ROOM_FULL);
        }
        validateUserNotInRoom(userId);
    }

    /**
     * 방 내부 스냅샷 조회
     */
    public RoomSnapshotEvent snapshot(String roomId, Long userId) {
        validateRoomId(roomId);
        Room room = getRoomOrThrow(roomId);
        getPlayerInRoomOrThrow(userId, roomId);
        return RoomSnapshotEvent.of(RoomInfoResponse.of(room), buildPlayerInfoList(room));
    }

    private List<PlayerInfo> buildPlayerInfoList(Room room) {
        return playerRepository.findAllByRoomId(room.getRoomId()).stream()
                .map(p -> PlayerInfo.of(p, room.getHostId()))
                .toList();
    }

    /**
     * 플레이어 방 퇴장
     */
    public LeaveRoomResult leaveRoom(String roomId, Long userId) {
        validateRoomId(roomId);
        AtomicBoolean roomDeleted = new AtomicBoolean(false);
        AtomicBoolean hostChanged = new AtomicBoolean(false);
        LeaveRoomResult leaveRoomResult = roomLockManager.executeWithLock(roomId, () -> {
            Room room = getRoomOrThrow(roomId);
            Player player = getPlayerInRoomOrThrow(userId, room.getRoomId());
            PlayerLeftEvent playerLeftEvent = PlayerLeftEvent.of(player, room.getHostId());

            if (getPlayerCountInRoom(roomId) <= 1) {
                roomRepository.deleteById(roomId);
                playerRepository.deleteById(userId);
                roomDeleted.set(true);
                return LeaveRoomResult.of(playerLeftEvent);
            }

            playerRepository.deleteById(userId);

            if (room.getHostId().equals(userId)) {
                List<Player> remainingPlayers = findRemainingPlayers(roomId);
                if (remainingPlayers.isEmpty()) {
                    roomDeleted.set(true);
                    return LeaveRoomResult.of(playerLeftEvent);
                }
                HostChangedEvent hostChangedEvent = changeHost(room, player, remainingPlayers);
                hostChanged.set(true);
                return LeaveRoomResult.of(playerLeftEvent, hostChangedEvent);
            }

            return LeaveRoomResult.of(playerLeftEvent);
        });

        log.info("방 퇴장 처리됨 - action=room-left, roomId={}, userId={}, roomDeleted={}, hostChanged={}",
                roomId, userId, roomDeleted.get(), hostChanged.get());
        return leaveRoomResult;
    }

    /**
     * 호스트 변경
     */
    private HostChangedEvent changeHost(Room room, Player previousHost, List<Player> remainingPlayers) {
        Player newHost = remainingPlayers.stream()
                .min(Comparator.comparing(Player::getCreatedAt))
                .orElseThrow(() -> new CustomException(PLAYER_NOT_IN_ANY_ROOM));
        newHost.unready();
        room.changeHost(newHost.getUserId());
        return HostChangedEvent.of(newHost, previousHost, room.getHostId(), HOST_CHANGED_REASON_HOST_LEFT);
    }

    /**
     * 플레이어 준비 완료
     */
    public PlayerReadyEvent readyPlayer(String roomId, Long userId) {
        validateRoomId(roomId);
        PlayerReadyEvent playerReadyEvent = roomLockManager.executeWithLock(roomId, () -> {
            Room room = getRoomOrThrow(roomId);
            Player player = getPlayerInRoomOrThrow(userId, room.getRoomId());
            validatePlayerNotHost(player, room);
            validateRoomNotInGame(room);

            player.ready();

            boolean isAllReady = isAllGuestsReady(room);
            return PlayerReadyEvent.of(player, room.getHostId(), isAllReady);
        });
        log.info("방 플레이어 준비 완료 - action=player-ready, roomId={}, userId={}, allReady={}",
                roomId, userId, playerReadyEvent.allReady());

        return playerReadyEvent;
    }

    private void validateRoomNotInGame(Room room) {
        if (room.isInGame()) {
            throw new CustomException(ROOM_IN_GAME);
        }
    }

    private boolean isAllGuestsReady(Room room) {
        List<Player> players = playerRepository.findAllByRoomId(room.getRoomId());
        for (var p : players) {
            if (room.getHostId().equals(p.getUserId())) {
                continue; // 호스트는 준비 상태가 없음
            }

            if (!p.isReady()) {
                return false;
            }
        }
        return true;
    }

    private void validatePlayerNotHost(Player player, Room room) {
        if (room.getHostId().equals(player.getUserId())) {
            throw new CustomException(HOST_CANNOT_READY); // 호스트는 준비 상태가 없음
        }
    }

    private void validatePlayerHost(Player player, Room room) {
        if (!room.getHostId().equals(player.getUserId())) {
            throw new CustomException(ONLY_HOST_CAN_START_GAME);
        }
    }

    /**
     * 플레이어 준비 취소
     */
    public PlayerUnreadyEvent unreadyPlayer(String roomId, Long userId) {
        validateRoomId(roomId);
        PlayerUnreadyEvent playerUnreadyEvent = roomLockManager.executeWithLock(roomId, () -> {
            Room room = getRoomOrThrow(roomId);
            Player player = getPlayerInRoomOrThrow(userId, room.getRoomId());
            validatePlayerNotHost(player, room);
            validateRoomNotInGame(room);
            player.unready();
            return PlayerUnreadyEvent.of(player, room.getHostId());
        });
        log.info("방 플레이어 준비 취소 - action=player-unready, roomId={}, userId={}",
                roomId, userId);
        return playerUnreadyEvent;
    }

    /**
     * 게임 시작
     */
    public GameStartEvent startGame(String roomId, Long userId) {
        validateRoomId(roomId);
        GameStartEvent gameStartEvent = roomLockManager.executeWithLock(roomId, () -> {
            Room room = getRoomOrThrow(roomId);
            Player player = getPlayerInRoomOrThrow(userId, room.getRoomId());
            validateRoomNotInGame(room);
            validatePlayerHost(player, room);
            validatePlayerCountOverMinimum(room);
            if (!isAllGuestsReady(room)) {
                throw new CustomException(PLAYERS_NOT_READY);
            }
            return gameService.startGame(room);
        });
        log.info("방 게임 시작됨 - action=game-started, roomId={}, gameId={}, hostId={}",
                roomId, gameStartEvent.gameId(), userId);
        return gameStartEvent;
    }

    private void validatePlayerCountOverMinimum(Room room) {
        int playerCount = getPlayerCountInRoom(room.getRoomId());
        if (playerCount < MIN_PLAYERS_TO_START) {
            throw new CustomException(NOT_ENOUGH_PLAYERS_TO_START);
        }
    }

    private Room getRoomOrThrow(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ROOM_NOT_FOUND));
    }

    private Player getPlayerInRoomOrThrow(Long userId, String roomId) {
        Player player = playerRepository.findById(userId)
                .orElseThrow(() -> new CustomException(PLAYER_NOT_IN_ANY_ROOM));

        if (!player.getRoomId().equals(roomId)) {
            throw new CustomException(PLAYER_NOT_IN_ROOM);
        }

        return player;
    }

    private List<Player> findRemainingPlayers(String roomId) {
        return playerRepository.findAllByRoomId(roomId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

}
