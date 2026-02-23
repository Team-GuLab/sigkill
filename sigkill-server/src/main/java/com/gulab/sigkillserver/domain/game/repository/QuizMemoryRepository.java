package com.gulab.sigkillserver.domain.game.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulab.sigkillserver.common.exception.CustomException;
import com.gulab.sigkillserver.domain.game.exception.QuizErrorCode;
import com.gulab.sigkillserver.domain.game.model.quiz.Quiz;
import com.gulab.sigkillserver.domain.game.model.quiz.QuizChoice;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

@Repository
public class QuizMemoryRepository implements QuizRepository {

    private static final String QUIZ_FILE_PATH = "quiz/quiz.json";
    private final Map<Long, Quiz> quizzesById;
    private final Map<String, List<Quiz>> quizzesByCategoryId;

    public QuizMemoryRepository(
            ObjectMapper objectMapper,
            @Value("classpath:" + QUIZ_FILE_PATH) Resource quizResource
    ) {
        Quiz[] quizzes = loadQuizzes(objectMapper, quizResource);
        this.quizzesById = buildQuizzesById(quizzes);
        this.quizzesByCategoryId = buildQuizzesByCategoryId(quizzes);
    }

    @Override
    public List<Quiz> findByCategoryId(String categoryId, int count) {
        if (categoryId == null || count <= 0) {
            return List.of();
        }

        List<Quiz> quizzes = quizzesByCategoryId.get(categoryId);
        if (quizzes == null || quizzes.isEmpty()) {
            return List.of();
        }

        List<Quiz> shuffled = new ArrayList<>(quizzes);
        shuffleInPlace(shuffled);

        return shuffled.stream()
                .limit(Math.min(count, shuffled.size()))
                .sorted(Comparator.comparingInt(Quiz::difficulty).thenComparingLong(Quiz::quizId))
                .toList();
    }

    @Override
    public Optional<Quiz> findById(long quizId) {
        return Optional.ofNullable(quizzesById.get(quizId));
    }

    private Quiz[] loadQuizzes(ObjectMapper objectMapper, Resource quizResource) {
        try (InputStream inputStream = quizResource.getInputStream()) {
            Quiz[] quizzes = objectMapper.readValue(inputStream, Quiz[].class);
            if (quizzes == null) {
                throw new CustomException(QuizErrorCode.QUIZ_CATALOG_EMPTY);
            }
            return quizzes;
        } catch (IOException e) {
            throw new CustomException(QuizErrorCode.QUIZ_CATALOG_LOAD_FAILED);
        }
    }

    private Map<Long, Quiz> buildQuizzesById(Quiz[] quizzes) {
        Map<Long, Quiz> byId = new HashMap<>();

        for (Quiz quiz : quizzes) {
            validateQuiz(quiz);
            Quiz existingQuiz = byId.putIfAbsent(quiz.quizId(), quiz);
            if (existingQuiz != null) {
                throw new CustomException(QuizErrorCode.QUIZ_ID_DUPLICATED);
            }
        }

        return Map.copyOf(byId);
    }

    private Map<String, List<Quiz>> buildQuizzesByCategoryId(Quiz[] quizzes) {
        Map<String, List<Quiz>> byCategoryId = new HashMap<>();
        for (Quiz quiz : quizzes) {
            byCategoryId.computeIfAbsent(quiz.categoryId(), key -> new ArrayList<>()).add(quiz);
        }

        return byCategoryId.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())
                ));
    }

    private void validateQuiz(Quiz quiz) {
        if (quiz == null) {
            throw new CustomException(QuizErrorCode.QUIZ_DATA_NULL);
        }

        if (quiz.categoryId() == null || quiz.categoryId().isBlank()) {
            throw new CustomException(QuizErrorCode.QUIZ_CATEGORY_INVALID);
        }

        if (quiz.choices() == null || quiz.choices().isEmpty()) {
            throw new CustomException(QuizErrorCode.QUIZ_CHOICES_INVALID);
        }

        long distinctChoiceCount = quiz.choices().stream()
                .map(QuizChoice::choiceId)
                .distinct()
                .count();

        if (distinctChoiceCount != quiz.choices().size()) {
            throw new CustomException(QuizErrorCode.QUIZ_CHOICE_ID_DUPLICATED);
        }

        boolean hasCorrectChoice = quiz.choices().stream()
                .anyMatch(choice -> choice.choiceId() == quiz.correctChoiceId());

        if (!hasCorrectChoice) {
            throw new CustomException(QuizErrorCode.QUIZ_CORRECT_CHOICE_INVALID);
        }
    }

    private void shuffleInPlace(List<Quiz> quizzes) {
        for (int i = quizzes.size() - 1; i > 0; i--) {
            int swapIndex = ThreadLocalRandom.current().nextInt(i + 1);
            Quiz temp = quizzes.get(i);
            quizzes.set(i, quizzes.get(swapIndex));
            quizzes.set(swapIndex, temp);
        }
    }
}
