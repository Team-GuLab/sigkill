package com.gulab.sigkillserver.domain.game.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gulab.sigkillserver.domain.game.model.quiz.QuizChoiceNumberMapping;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QuizChoiceNumberMappingMemoryRepositoryTest {

    private QuizChoiceNumberMappingRepository repository;

    @BeforeEach
    void setUp() {
        repository = new QuizChoiceNumberMappingMemoryRepository();
    }

    @Nested
    class SaveAndFindTests {

        @Test
        void gameId와_quizId로_매핑을_저장하고_조회할_수_있다() {
            // given
            QuizChoiceNumberMapping mapping = QuizChoiceNumberMapping.create(
                    1L,
                    1001L,
                    Map.of(1, 11L, 2, 12L, 3, 13L, 4, 14L)
            );

            // when
            repository.save(mapping);

            // then
            assertThat(repository.findByGameIdAndQuizId(1L, 1001L)).contains(mapping);
        }

        @Test
        void number로_choiceId를_조회할_수_있다() {
            // given
            QuizChoiceNumberMapping mapping = QuizChoiceNumberMapping.create(
                    1L,
                    1001L,
                    Map.of(1, 99L, 2, 77L)
            );

            // when
            Long choiceId = mapping.findChoiceIdByNumber(2).orElseThrow();

            // then
            assertThat(choiceId).isEqualTo(77L);
            assertThat(mapping.findChoiceIdByNumber(999)).isEmpty();
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void gameId와_quizId로_삭제할_수_있다() {
            // given
            repository.save(QuizChoiceNumberMapping.create(1L, 1001L, Map.of(1, 11L)));
            repository.save(QuizChoiceNumberMapping.create(1L, 1002L, Map.of(1, 12L)));

            // when
            repository.deleteByGameIdAndQuizId(1L, 1001L);

            // then
            assertThat(repository.findByGameIdAndQuizId(1L, 1001L)).isEmpty();
            assertThat(repository.findByGameIdAndQuizId(1L, 1002L)).isPresent();
        }

        @Test
        void gameId로_해당_게임의_모든_매핑을_삭제할_수_있다() {
            // given
            repository.save(QuizChoiceNumberMapping.create(1L, 1001L, Map.of(1, 11L)));
            repository.save(QuizChoiceNumberMapping.create(1L, 1002L, Map.of(1, 12L)));
            repository.save(QuizChoiceNumberMapping.create(2L, 2001L, Map.of(1, 21L)));

            // when
            repository.deleteByGameId(1L);

            // then
            assertThat(repository.findByGameIdAndQuizId(1L, 1001L)).isEmpty();
            assertThat(repository.findByGameIdAndQuizId(1L, 1002L)).isEmpty();
            assertThat(repository.findByGameIdAndQuizId(2L, 2001L)).isPresent();
        }
    }

    @Nested
    class ModelGuardTests {

        @Test
        void 외부_map_변경이_내부_매핑에_영향을_주지_않는다() {
            // given
            Map<Integer, Long> source = new HashMap<>(Map.of(1, 11L, 2, 12L));
            QuizChoiceNumberMapping mapping = QuizChoiceNumberMapping.create(1L, 1001L, source);

            // when
            source.put(3, 13L);

            // then
            assertThat(mapping.getNumberToChoiceId()).hasSize(2);
            assertThat(mapping.getNumberToChoiceId()).doesNotContainKey(3);
        }

        @Test
        void 빈_map으로_생성하면_예외가_발생한다() {
            // given when then
            assertThatThrownBy(() -> QuizChoiceNumberMapping.create(1L, 1001L, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("numberToChoiceId");
        }
    }
}
