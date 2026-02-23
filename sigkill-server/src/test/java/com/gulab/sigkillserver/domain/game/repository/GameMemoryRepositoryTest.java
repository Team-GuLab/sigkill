package com.gulab.sigkillserver.domain.game.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.exception.GameErrorCode;
import com.gulab.sigkillserver.domain.game.model.Game;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GameMemoryRepositoryTest {

    private GameRepository gameRepository;

    @BeforeEach
    void setUp() {
        gameRepository = new GameMemoryRepository();
    }

    @Nested
    class SaveAndFindTests {

        @Test
        void 게임을_저장하고_roomId로_조회할_수_있다() {
            // given
            Game game = Game.create(1L, "1234", List.of(11L, 12L));

            // when
            gameRepository.save(game);

            // then
            assertThat(gameRepository.findByRoomId("1234")).contains(game);
        }

        @Test
        void 같은_roomId에_게임을_중복_저장하면_예외가_발생한다() {
            // given
            gameRepository.save(Game.create(1L, "1234", List.of(11L, 12L)));

            // when then
            assertThatThrownBy(() -> gameRepository.save(Game.create(2L, "1234", List.of(21L, 22L))))
                    .isInstanceOf(CustomException.class)
                    .satisfies(throwable -> assertThat(((CustomException) throwable).getErrorCode().getCode())
                            .isEqualTo(GameErrorCode.GAME_ALREADY_EXISTS.name()));
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void 게임을_삭제하면_조회되지_않는다() {
            // given
            gameRepository.save(Game.create(1L, "1234", List.of(11L, 12L)));

            // when
            gameRepository.deleteByRoomId("1234");

            // then
            assertThat(gameRepository.findByRoomId("1234")).isEmpty();
        }
    }
}
