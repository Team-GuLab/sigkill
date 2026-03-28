package com.gulab.sigkillserver.domain.room.service;

import com.gulab.sigkillserver.domain.room.model.Player;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.user.model.User;
import com.gulab.sigkillserver.domain.user.model.UserRole;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import java.util.List;

public class RoomClosingStateManager {

    private RoomClosingStateManager() {
    }

    public static void updateClosingState(Room room, PlayerRepository playerRepository, UserRepository userRepository) {
        if (room.isInGame()) {
            room.clearClosing();
            return;
        }

        List<Player> players = playerRepository.findAllByRoomId(room.getRoomId());
        boolean hasOnlyBots = !players.isEmpty() && players.stream()
                .noneMatch(player -> isHumanPlayer(player, userRepository));

        if (hasOnlyBots) {
            room.markClosing();
            return;
        }

        room.clearClosing();
    }

    private static boolean isHumanPlayer(Player player, UserRepository userRepository) {
        return userRepository.findById(player.getUserId())
                .map(User::getRole)
                .map(role -> role != UserRole.BOT)
                .orElse(true);
    }
}
