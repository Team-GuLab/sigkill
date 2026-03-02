package com.gulab.sigkillserver.domain.room.service;

import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.DEFAULT_CAPACITY;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_CAPACITY;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MAX_TITLE_LENGTH;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MIN_CAPACITY;
import static com.gulab.sigkillserver.domain.room.constant.RoomConstants.MIN_PLAYERS_TO_START;
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
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_JOIN_RESERVATION_INVALID;
import static com.gulab.sigkillserver.domain.room.exception.RoomErrorCode.ROOM_JOIN_RESERVATION_NOT_FOUND;
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
import com.gulab.sigkillserver.domain.room.dto.rest.response.ReserveJoinResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomAvailabilityResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomCreateResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomListResponse;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomResponse;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.HostChangedEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerJoinEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerLeftEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerReadyEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.event.PlayerUnreadyEvent;
import com.gulab.sigkillserver.domain.room.dto.stomp.shared.PlayerInfo;
import com.gulab.sigkillserver.domain.room.model.PendingJoin;
import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.PendingJoinRepository;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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
    private static final long PENDING_JOIN_TTL_MILLIS = 5_000L;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final PendingJoinRepository pendingJoinRepository;

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

        int totalCount = (int) roomRepository.count();
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

        // Repository에서 정렬 및 페이징 처리
        List<RoomResponse> roomResponses = roomRepository.findAll(comparator, offset, size)
                .stream()
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
    public RoomCreateResponse createRoom(String roomTitle, Integer capacity, Long userId) {
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

                log.info("room.create success - roomId={}, hostId={}, capacity={}", roomId, userId, resolvedCapacity);
                return RoomCreateResponse.of(room, hostPlayer);
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
        return String.valueOf(ThreadLocalRandom.current().nextInt(1000, 10000));
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

    /**
     * 방 참가 가능 여부 확인
     */
    public RoomAvailabilityResponse checkRoomAvailability(String roomId, Long userId) {
        validateRoomId(roomId);
        Room room = getRoomOrThrow(roomId);
        // TODO: 동시성 문제 해결
        validateCanJoinRoom(userId, room);

        return new RoomAvailabilityResponse(room.getRoomId(), true);
    }

    /**
     * 방 참가 예약
     */
    public ReserveJoinResponse reserveJoin(String roomId, Long userId) {
        validateRoomId(roomId);
        getUserOrThrow(userId);

        PendingJoin reservedPendingJoin = roomLockManager.executeWithLock(roomId, () -> {
            long now = Instant.now().toEpochMilli();
            pendingJoinRepository.deleteExpired(now);

            Room room = getRoomOrThrow(roomId);
            validateCanJoinRoom(userId, room);

            PendingJoin existingPendingJoin = pendingJoinRepository.findByRoomIdAndUserId(roomId, userId)
                    .filter(existing -> !existing.isExpiredAt(now))
                    .orElse(null);
            if (existingPendingJoin != null) {
                return existingPendingJoin;
            }

            if (isRoomFullForReserve(room, now)) {
                throw new CustomException(ROOM_FULL);
            }

            String createdJoinTxId = UUID.randomUUID().toString();
            PendingJoin createdPendingJoin = PendingJoin.create(
                    createdJoinTxId,
                    roomId,
                    userId,
                    now,
                    now + PENDING_JOIN_TTL_MILLIS
            );
            pendingJoinRepository.save(createdPendingJoin);
            return createdPendingJoin;
        });

        long now = Instant.now().toEpochMilli();
        long ttlMillis = Math.max(0L, reservedPendingJoin.expiresAtMillis() - now);

        log.info("room.reserveJoin success - roomId={}, userId={}, joinTxId={}",
                roomId, userId, reservedPendingJoin.joinTxId());
        return new ReserveJoinResponse(
                reservedPendingJoin.joinTxId(),
                reservedPendingJoin.expiresAtMillis(),
                ttlMillis
        );
    }

    /**
     * 방 참가 확정 (PENDING -> ACTIVE)
     */
    public PlayerJoinEvent confirmJoin(String roomId, Long userId, String joinTxId) {
        validateRoomId(roomId);
        Objects.requireNonNull(joinTxId, "joinTxId must not be null");
        User user = getUserOrThrow(userId);

        return roomLockManager.executeWithLock(roomId, () -> {
            long now = Instant.now().toEpochMilli();
            pendingJoinRepository.deleteExpired(now);

            Room room = getRoomOrThrow(roomId);
            validateRoomNotInGame(room);

            // 멱등성을 위한 처리
            Player existingPlayer = playerRepository.findById(userId).orElse(null);
            if (existingPlayer != null) {
                if (!existingPlayer.getRoomId().equals(roomId)) {
                    throw new CustomException(USER_ALREADY_IN_ROOM);
                }
                return buildPlayerJoinEvent(room);
            }

            PendingJoin pendingJoin = pendingJoinRepository.findByJoinTxId(joinTxId)
                    .orElseThrow(() -> new CustomException(ROOM_JOIN_RESERVATION_NOT_FOUND));

            if (!pendingJoin.roomId().equals(roomId) || !pendingJoin.userId().equals(userId)) {
                throw new CustomException(ROOM_JOIN_RESERVATION_INVALID);
            }
            if (pendingJoin.isExpiredAt(now)) {
                pendingJoinRepository.deleteByJoinTxId(joinTxId);
                throw new CustomException(ROOM_JOIN_RESERVATION_NOT_FOUND);
            }
            if (isRoomFull(room)) {
                throw new CustomException(ROOM_FULL);
            }

            Player player = Player.create(userId, roomId, user.getNickname());
            playerRepository.create(player);
            pendingJoinRepository.deleteByJoinTxId(joinTxId);

            return buildPlayerJoinEvent(room);
        });
    }

    private boolean isRoomFullForReserve(Room room, long nowEpochMillis) {
        int activePlayers = getPlayerCountInRoom(room.getRoomId());
        int pendingPlayers = pendingJoinRepository.countUnexpiredByRoomId(room.getRoomId(), nowEpochMillis);
        return activePlayers + pendingPlayers >= room.getCapacity();
    }

    private void validateRoomId(String roomId) {
        int roomIdInt;
        try {
            roomIdInt = Integer.parseInt(roomId);
        } catch (NumberFormatException e) {
            throw new CustomException(ROOM_NUMBER_ERROR);
        }
        if (roomIdInt < 1000 || roomIdInt > 9999) {
            throw new CustomException(ROOM_NUMBER_ERROR);
        }
    }

    /**
     * 플레이어 방 참가
     */
    public PlayerJoinEvent joinRoom(String roomId, Long userId) {
        validateRoomId(roomId);
        User user = getUserOrThrow(userId);

        Room room = getRoomOrThrow(roomId);

        validateCanJoinRoom(userId, room);

        Player player = Player.create(userId, roomId, user.getNickname());
        playerRepository.create(player);

        PlayerJoinEvent playerJoinEvent = buildPlayerJoinEvent(room);
        log.info("room.join success - roomId={}, userId={}, players={}", roomId, userId,
                playerJoinEvent.players().size());
        return playerJoinEvent;
    }

    private void validateCanJoinRoom(Long userId, Room room) {
        if (room.isInGame()) {
            throw new CustomException(ROOM_IN_GAME);
        }
        if (isRoomFull(room)) {
            throw new CustomException(ROOM_FULL);
        }
        if (playerRepository.findById(userId).isPresent()) {
            throw new CustomException(USER_ALREADY_IN_ROOM);
        }
    }

    private PlayerJoinEvent buildPlayerJoinEvent(Room room) {
        List<PlayerInfo> playerInfos = playerRepository.findAllByRoomId(room.getRoomId()).stream()
                .map(player -> PlayerInfo.of(player, room.getHostId()))
                .toList();
        return PlayerJoinEvent.of(room, playerInfos);
    }

    /**
     * 플레이어 방 퇴장
     */
    public LeaveRoomResult leaveRoom(String roomId, Long userId) {
        Room room = getRoomOrThrow(roomId);
        Player player = getPlayerInRoomOrThrow(userId, room.getRoomId());
        PlayerLeftEvent playerLeftEvent = PlayerLeftEvent.of(player, room.getHostId());

        if (getPlayerCountInRoom(roomId) <= 1) {
            roomRepository.deleteById(roomId);
            playerRepository.deleteById(userId);
            pendingJoinRepository.deleteByRoomId(roomId);
            log.info("room.leave success - roomId={}, userId={}, roomDeleted=true", roomId, userId);
            return LeaveRoomResult.of(playerLeftEvent);
        }

        playerRepository.deleteById(userId);
        pendingJoinRepository.deleteByUserId(userId);

        if (room.getHostId().equals(userId)) {
            HostChangedEvent hostChangedEvent = changeHost(room, player);
            log.info("room.leave success - roomId={}, userId={}, hostChanged=true", roomId, userId);
            return LeaveRoomResult.of(playerLeftEvent, hostChangedEvent);
        }

        log.info("room.leave success - roomId={}, userId={}, hostChanged=false", roomId, userId);
        return LeaveRoomResult.of(playerLeftEvent);
    }

    /**
     * disconnect 발생 시 확정되지 않은 입장 예약 정리
     */
    public boolean rollbackPendingJoinOnDisconnect(Long userId) {
        PendingJoin pendingJoin = pendingJoinRepository.findByUserId(userId).orElse(null);
        if (pendingJoin == null) {
            return false;
        }

        return roomLockManager.executeWithLock(pendingJoin.roomId(), () -> {
            PendingJoin current = pendingJoinRepository.findByJoinTxId(pendingJoin.joinTxId()).orElse(null);
            if (current == null || !current.userId().equals(userId)) {
                return false;
            }

            pendingJoinRepository.deleteByJoinTxId(current.joinTxId());
            log.info("room.pending.rollback success - roomId={}, userId={}, joinTxId={}",
                    current.roomId(), userId, current.joinTxId());
            return true;
        });
    }

    /**
     * 호스트 변경
     */
    private HostChangedEvent changeHost(Room room, Player previousHost) {
        Player newHost = playerRepository.findAllByRoomId(room.getRoomId()).stream()
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
        Room room = getRoomOrThrow(roomId);
        Player player = getPlayerInRoomOrThrow(userId, room.getRoomId());
        validatePlayerNotHost(player, room);
        validateRoomNotInGame(room);

        player.ready();

        boolean isAllReady = isAllGuestsReady(room);

        log.info("room.ready success - roomId={}, userId={}, allReady={}", roomId, userId, isAllReady);
        return PlayerReadyEvent.of(player, room.getHostId(), isAllReady);
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
        Room room = getRoomOrThrow(roomId);
        Player player = getPlayerInRoomOrThrow(userId, room.getRoomId());
        validatePlayerNotHost(player, room);
        validateRoomNotInGame(room);

        player.unready();

        log.info("room.unready success - roomId={}, userId={}", roomId, userId);
        return PlayerUnreadyEvent.of(player, room.getHostId());
    }

    /**
     * 게임 시작
     */
    public GameStartEvent startGame(String roomId, Long userId) {
        Room room = getRoomOrThrow(roomId);
        Player player = getPlayerInRoomOrThrow(userId, room.getRoomId());
        validateRoomNotInGame(room);
        validatePlayerHost(player, room);
        validatePlayerCountOverMinimum(room);

        if (!isAllGuestsReady(room)) {
            throw new CustomException(PLAYERS_NOT_READY);
        }

        return gameService.startGame(room);
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

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }
}
