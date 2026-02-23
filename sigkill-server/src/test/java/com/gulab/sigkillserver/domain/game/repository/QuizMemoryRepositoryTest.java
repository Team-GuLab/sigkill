package com.gulab.sigkillserver.domain.game.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.exception.QuizErrorCode;
import com.gulab.sigkillserver.domain.game.model.quiz.Quiz;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

class QuizMemoryRepositoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    class FindByCategoryIdTests {

        @Test
        void 카테고리와_개수로_조회하면_랜덤_추출_후_난이도_오름차순_목록을_반환한다() {
            // given
            QuizMemoryRepository repository = createRepositoryWithDefaultResource();

            // when
            List<Quiz> quizzes = repository.findByCategoryId("CS", 5);

            // then
            assertThat(quizzes).hasSize(5);
            assertThat(quizzes).allMatch(quiz -> quiz.categoryId().equals("CS"));
            assertThat(quizzes)
                    .extracting(Quiz::difficulty)
                    .isSorted();
        }

        @Test
        void 요청_개수가_보유_문제보다_많으면_가능한_개수만큼_반환한다() {
            // given
            QuizMemoryRepository repository = createRepositoryWithDefaultResource();

            // when
            List<Quiz> quizzes = repository.findByCategoryId("CS", 100);

            // then
            assertThat(quizzes).hasSize(10);
            assertThat(quizzes).allMatch(quiz -> quiz.categoryId().equals("CS"));
            assertThat(quizzes)
                    .extracting(Quiz::difficulty)
                    .isSorted();
        }

        @Test
        void 없는_카테고리로_조회하면_빈_목록을_반환한다() {
            // given
            QuizMemoryRepository repository = createRepositoryWithDefaultResource();

            // when
            List<Quiz> quizzes = repository.findByCategoryId("UNKNOWN", 5);

            // then
            assertThat(quizzes).isEmpty();
        }

        @Test
        void count가_0_이하면_빈_목록을_반환한다() {
            // given
            QuizMemoryRepository repository = createRepositoryWithDefaultResource();

            // when
            List<Quiz> quizzes = repository.findByCategoryId("CS", 0);

            // then
            assertThat(quizzes).isEmpty();
        }
    }

    @Nested
    class FindByIdTests {

        @Test
        void 존재하는_quizId로_조회하면_퀴즈를_반환한다() {
            // given
            QuizMemoryRepository repository = createRepositoryWithDefaultResource();

            // when
            var quiz = repository.findById(1L);

            // then
            assertThat(quiz).isPresent();
            assertThat(quiz.orElseThrow().quizId()).isEqualTo(1L);
            assertThat(quiz.orElseThrow().categoryId()).isEqualTo("CS");
        }

        @Test
        void 존재하지_않는_quizId면_빈_Optional을_반환한다() {
            // given
            QuizMemoryRepository repository = createRepositoryWithDefaultResource();

            // when
            var quiz = repository.findById(99999L);

            // then
            assertThat(quiz).isEmpty();
        }
    }

    @Nested
    class LoadTests {

        @Test
        void 잘못된_json이면_시작시점에_실패한다() {
            // given
            Resource malformedResource = new ByteArrayResource("{invalid-json}".getBytes(StandardCharsets.UTF_8));

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> new QuizMemoryRepository(objectMapper, malformedResource),
                    QuizErrorCode.QUIZ_CATALOG_LOAD_FAILED.name()
            );
        }

        @Test
        void 중복_quizId가_있으면_시작시점에_실패한다() {
            // given
            Resource duplicateQuizIdResource = jsonResource("""
                    [
                      {
                        "quizId": 1,
                        "categoryId": "CS",
                        "question": "문제1",
                        "explanation": "해설1",
                        "correctChoiceId": 1,
                        "difficulty": 1,
                        "choices": [{ "choiceId": 1, "text": "선지1" }]
                      },
                      {
                        "quizId": 1,
                        "categoryId": "CS",
                        "question": "문제2",
                        "explanation": "해설2",
                        "correctChoiceId": 1,
                        "difficulty": 1,
                        "choices": [{ "choiceId": 1, "text": "선지1" }]
                      }
                    ]
                    """);

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> new QuizMemoryRepository(objectMapper, duplicateQuizIdResource),
                    QuizErrorCode.QUIZ_ID_DUPLICATED.name()
            );
        }

        @Test
        void correctChoiceId가_choices에_없으면_시작시점에_실패한다() {
            // given
            Resource invalidCorrectChoiceResource = jsonResource("""
                    [
                      {
                        "quizId": 1,
                        "categoryId": "CS",
                        "question": "문제1",
                        "explanation": "해설1",
                        "correctChoiceId": 99,
                        "difficulty": 1,
                        "choices": [{ "choiceId": 1, "text": "선지1" }]
                      }
                    ]
                    """);

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> new QuizMemoryRepository(objectMapper, invalidCorrectChoiceResource),
                    QuizErrorCode.QUIZ_CORRECT_CHOICE_INVALID.name()
            );
        }

        @Test
        void choiceId가_중복되면_시작시점에_실패한다() {
            // given
            Resource duplicateChoiceIdResource = jsonResource("""
                    [
                      {
                        "quizId": 1,
                        "categoryId": "CS",
                        "question": "문제1",
                        "explanation": "해설1",
                        "correctChoiceId": 1,
                        "difficulty": 1,
                        "choices": [
                          { "choiceId": 1, "text": "선지1" },
                          { "choiceId": 1, "text": "선지2" }
                        ]
                      }
                    ]
                    """);

            // when then
            assertThrowsCustomExceptionWithCode(
                    () -> new QuizMemoryRepository(objectMapper, duplicateChoiceIdResource),
                    QuizErrorCode.QUIZ_CHOICE_ID_DUPLICATED.name()
            );
        }
    }

    private void assertThrowsCustomExceptionWithCode(ThrowingCallable callable, String expectedCode) {
        assertThatThrownBy(callable)
                .isInstanceOf(CustomException.class)
                .satisfies(throwable -> assertThat(((CustomException) throwable)
                        .getErrorCode()
                        .getCode()).isEqualTo(expectedCode));
    }

    private QuizMemoryRepository createRepositoryWithDefaultResource() {
        return new QuizMemoryRepository(objectMapper, new ClassPathResource("quiz/quiz.json"));
    }

    private Resource jsonResource(String json) {
        return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));
    }
}
