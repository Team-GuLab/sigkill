package com.gulab.sigkillserver.domain.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gulab.sigkillserver.domain.game.repository.GameMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.GamePlayerMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.GamePlayerRepository;
import com.gulab.sigkillserver.domain.game.repository.GameRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizChoiceNumberMappingMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizChoiceNumberMappingRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.QuizRepository;
import com.gulab.sigkillserver.domain.game.repository.SelectedChoiceMemoryRepository;
import com.gulab.sigkillserver.domain.game.repository.SelectedChoiceRepository;
import com.gulab.sigkillserver.domain.game.service.GameEventBuilder;
import com.gulab.sigkillserver.domain.game.service.GameService;
import com.gulab.sigkillserver.domain.room.dto.rest.response.RoomCreateResponse;
import com.gulab.sigkillserver.domain.room.model.Room;
import com.gulab.sigkillserver.domain.room.repository.PlayerMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.PlayerRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomMemoryRepository;
import com.gulab.sigkillserver.domain.room.repository.RoomRepository;
import com.gulab.sigkillserver.domain.room.service.RoomService;
import com.gulab.sigkillserver.domain.user.dto.rest.response.LoginResponse;
import com.gulab.sigkillserver.domain.user.repository.UserMemoryRepository;
import com.gulab.sigkillserver.domain.user.repository.UserRepository;
import com.gulab.sigkillserver.domain.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;

public class ConsistencyRulesIntegrationTest {
    private UserRepository userRepository;
    private GameRepository gameRepository;
    private QuizRepository quizRepository;
    private PlayerRepository playerRepository;
    private RoomRepository roomRepository;
    private SelectedChoiceRepository selectedChoiceRepository;
    private QuizChoiceNumberMappingRepository quizChoiceNumberMappingRepository;
    private GamePlayerRepository gamePlayerRepository;
    private GameEventBuilder gameEventBuilder;

    private UserService userService;
    private GameService gameService;
    private RoomService roomService;

    @BeforeEach
    void initObjects() {
        userRepository = new UserMemoryRepository();
        gameRepository = new GameMemoryRepository();
        quizRepository = new QuizMemoryRepository(new ObjectMapper(), new ClassPathResource("quiz/quiz.json"));
        playerRepository = new PlayerMemoryRepository();
        roomRepository = new RoomMemoryRepository();
        selectedChoiceRepository = new SelectedChoiceMemoryRepository();
        quizChoiceNumberMappingRepository = new QuizChoiceNumberMappingMemoryRepository();
        gamePlayerRepository = new GamePlayerMemoryRepository();
        gameEventBuilder = new GameEventBuilder();

        userService = new UserService(userRepository);

        gameService = new GameService(
                userRepository,
                gameRepository,
                quizRepository,
                playerRepository,
                roomRepository,
                selectedChoiceRepository,
                quizChoiceNumberMappingRepository,
                gamePlayerRepository,
                gameEventBuilder
        );
        roomService = new RoomService(roomRepository, userRepository, playerRepository, gameService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private LoginResponse loginGuest(String sessionId) {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn(sessionId);

        return userService.loginAsGuest(session);
    }

    private <T> List<Throwable> runConcurrently(List<T> inputs, ThrowingConsumer<T> action)
            throws InterruptedException {
        int threadCount = inputs.size();
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        try {
            for (T input : inputs) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        action.accept(input);
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        done.countDown();
                    }
                });
            }

            ready.await();
            start.countDown();
            done.await();
            return errors;
        } finally {
            pool.shutdown();
        }
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T input) throws Exception;
    }

    @Nested
    class RoomCreateJoinConcurrencyTests {
        //        @RepeatedTest(10000)
        @Test
        void 동시에_여러_사용자가_방을_만들어도_서로_다른_방_번호가_발급된다() throws InterruptedException {
            // given
            int userCount = 6;
            List<Long> userIds = IntStream.range(0, userCount)
                    .mapToObj(i -> loginGuest("session" + i).userId())
                    .toList();
            Set<String> roomIds = ConcurrentHashMap.newKeySet();

            // when
            List<Throwable> errors = runConcurrently(userIds, userId -> {
                RoomCreateResponse res = roomService.createRoom("테스트 방", 6, userId);
                roomIds.add(res.roomId());
            });

            // then
            assertThat(errors).isEmpty();
            assertThat(roomIds).hasSize(userCount);
            assertThat(roomIds).doesNotContainNull();
            var createdRooms = roomRepository.findAll();
            assertThat((long) createdRooms.size()).isEqualTo(userCount);
            assertThat(createdRooms).extracting(Room::getHostId)
                    .containsExactlyInAnyOrderElementsOf(userIds);
        }

        @Test
        void 같은_사용자가_동시에_여러_번_방_만들기를_눌러도_방은_하나만_만들어진다() {
            // given

            // when

            // then
        }

        @Test
        void 동시에_여러_사용자가_입장해도_방_정원을_넘겨_입장되지_않는다() {
            // given

            // when

            // then
        }

        @Test
        void 게임_시작_요청과_입장_요청이_동시에_도착해도_시작_시점의_참가자_스냅샷과_실제_방_인원이_일치한다() {
            // given

            // when

            // then
        }
    }

    @Nested
    class RoomLeaveHostTransitionConcurrencyTests {
        @Test
        void 나가는_사용자와_들어오는_사용자가_겹쳐도_방_참가자_목록이_깨지지_않는다() {
            // given

            // when

            // then
        }

        @Test
        void 방_나가기_요청과_게임_시작_요청이_동시에_도착해도_게임_참가자와_방_인원_정보가_일치한다() {
            // given

            // when

            // then
        }

        @Test
        void 방장이_나가는_순간_게임_시작_요청이_겹쳐도_새_방장이_정상적으로_결정된다() {
            // given

            // when

            // then
        }

        @Test
        void 방장_퇴장과_자동_퇴장이_동시에_발생해도_방장_변경_안내는_한_번만_전달된다() {
            // given

            // when

            // then
        }
    }

    @Nested
    class ReadyStartBoundaryTests {
        @Test
        void 준비_완료와_퇴장이_동시에_일어나도_시작_가능_여부는_최종_참가자_기준으로_계산된다() {
            // given

            // when

            // then
        }

        @Test
        void 준비_취소와_게임_시작_요청이_동시에_일어나면_준비_취소가_반영된_경우_게임_시작이_거부된다() {
            // given

            // when

            // then
        }
    }

    @Nested
    class GameLoadEndBoundaryTests {
        @Test
        void 참가자들이_동시에_게임_화면_로딩을_완료해도_전체_로딩_완료는_한_번만_확정된다() {
            // given

            // when

            // then
        }

        @Test
        void 게임_종료와_로딩_완료가_겹쳐도_종료된_게임이_다시_로딩_완료로_보이지_않는다() {
            // given

            // when

            // then
        }
    }

    @Nested
    class RoundTransitionConcurrencyTests {
        @Test
        void 라운드_종료와_다음_라운드_시작이_겹쳐도_문제_순서가_중복되거나_건너뛰지_않는다() {
            // given

            // when

            // then
        }

        @Test
        void 같은_라운드의_종료_처리_요청이_중복되어도_결과_집계는_한_번만_수행된다() {
            // given

            // when

            // then
        }
    }

    @Nested
    class SubmitScoringEndBoundaryTests {
        @Test
        void 한_사용자가_답을_연속으로_제출하면_가장_마지막_제출만_최종_답으로_인정된다() {
            // given

            // when

            // then
        }

        @Test
        void 답_제출과_라운드_종료가_동시에_발생해도_채점_결과는_요청_순서에_따라_흔들리지_않는다() {
            // given

            // when

            // then
        }

        @Test
        void 라운드_종료_시점과_제출_요청이_경계에서_겹쳐도_종료_이후_제출은_채점에_반영되지_않는다() {
            // given

            // when

            // then
        }

        @Test
        void 게임_종료와_답_제출이_동시에_발생해도_종료된_gameId에_제출_데이터가_남지_않는다() {
            // given

            // when

            // then
        }
    }
}
