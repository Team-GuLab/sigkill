# sigkill-server

실시간 방(룸) 기반 게임 로비를 위한 Spring Boot 서버입니다.

## 빠른 시작

### 요구 사항

- Java 21
- Gradle Wrapper 사용 (`./gradlew`)

### 실행

```bash
./gradlew bootRun
```

Redis 세션을 사용하므로 Redis가 먼저 떠 있어야 합니다.

```bash
docker run -d --name sigkill-redis -p 6379:6379 redis:7-alpine
./gradlew bootRun
```

Redis 접속 정보는 환경변수로 덮어쓸 수 있습니다.

```bash
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=
```

앱과 Redis를 함께 컨테이너로 띄우려면 아래를 사용합니다.

```bash
./gradlew clean build
docker compose up --build
```

### 테스트

```bash
./gradlew test
```

## 주요 링크

- WebSocket 테스트 페이지: [http://localhost:8080/room-ws-test.html](http://localhost:8080/room-ws-test.html)
- Swagger UI: [http://localhost:8080/swagger-ui/index.html#/](http://localhost:8080/swagger-ui/index.html#/)
- STOMP 계약 문서: [docs/STOMP_MESSAGE_SPEC.md](docs/STOMP_MESSAGE_SPEC.md)

## 정적 퀴즈 데이터

- 파일 위치: `src/main/resources/quiz/quiz.json`
- 로딩 시점: 애플리케이션 시작 시 1회 로딩
- 관리 방식: `QuizMemoryRepository`가 시작 시 메모리 `Map` 인덱스로 로드 후 조회
- 로딩 실패/스키마 오류: 시작 단계에서 예외를 발생시켜 fail-fast
- 예외 코드: 로드/검증 실패는 `QuizErrorCode` 기반 `CustomException` 사용

## REST 기능

### 인증

- `POST /api/v1/users/guest-login`
  - Redis 기반 HTTP 세션 게스트 로그인

### 방 관리

- `GET /api/v1/rooms`
  - 방 목록 조회 (페이징)
- `POST /api/v1/rooms`
  - 방 생성
  - 응답 `result` 예시:

```json
{
  "room": {
    "roomId": "5340",
    "roomTitle": "새 퀴즈방",
    "hostId": 69,
    "capacity": 6,
    "status": "WAITING"
  }
}
```

- `POST /api/v1/rooms/{roomId}/join`
  - 방 입장
  - 응답 `result` 형식은 방 생성과 동일하게 `{"room": {...}}`

## STOMP 기능

### 연결 규칙

- Endpoint: `/ws`
- App Prefix: `/app`
- Broker Prefix: `/topic`, `/queue`
- 인증 기준 쿠키: `JSESSIONID`

### Room 명령

- `SEND /app/room/join`
- `SEND /app/room/leave`
- `SEND /app/room/ready`
- `SEND /app/room/unready`

### 구독 채널

- Room 브로드캐스트: `SUBSCRIBE /topic/room/{roomId}`
- 사용자 에러: `SUBSCRIBE /user/queue/errors`
- 사용자 pong: `SUBSCRIBE /user/queue/pong`

### 이벤트 타입

- `PLAYER_JOIN`
- `PLAYER_LEFT`
- `HOST_CHANGED`
- `PLAYER_READY`
- `PLAYER_UNREADY`
- `PONG`
- `ERROR` (예외 응답 type)

## Redis 세션 운영 메모

- HTTP 세션은 Redis에 저장되므로 non-sticky 환경에서도 인증 세션 자체는 공유됩니다.
- WebSocket/STOMP 연결도 동일한 `JSESSIONID`를 기준으로 인증됩니다.
- 현재 `User`, `Room`, `Player`, `Game` 저장소는 여전히 WAS 로컬 메모리입니다.
- 따라서 다른 WAS로 라우팅되면 인증은 복원돼도 `USER_NOT_FOUND`, 방/게임 상태 불일치가 발생할 수 있습니다.
- 이 제한은 도메인 저장소를 외부 저장소로 이전하기 전까지 유지됩니다.

## 최근 추가된 실시간 안정성 기능

### 1) STOMP Heartbeat

- `SimpleBroker` heartbeat 활성화
- 값: `10000ms / 10000ms` (server->client / client->server)

### 2) Ping / Pong

- `SEND /app/ping` -> `SUBSCRIBE /user/queue/pong`
- 응답 필드: `type`, `userId`, `serverTime(epoch milliseconds)`

### 3) 비정상 종료 자동 퇴장 처리

- 클라이언트가 `leave` 없이 연결이 끊겨도 서버가 자동으로 퇴장 처리
- 필요 시 `PLAYER_LEFT`, `HOST_CHANGED`를 기존 room topic으로 브로드캐스트

### 4) SUBSCRIBE 인가 강화

- `/topic/room/{roomId}` 구독 허용:
  - 현재 해당 방 멤버
- `/topic/room/{roomId}` 구독 거부:
  - 현재 해당 방 멤버가 아닌 사용자(미참가/다른 방 참가)
- 사용자 큐는 허용 목록만 구독 가능:
  - `/user/queue/errors`
  - `/user/queue/pong`

### 5) 게임 타이머 전용 TaskScheduler

- 게임 라운드 타이머 용도로 별도 스케줄러 빈 사용
- 빈 이름: `gameTaskScheduler`
- 구현 클래스: `ThreadPoolTaskScheduler`
- 설정값:
  - `poolSize = 4`
  - `threadNamePrefix = game-task-`

## WebSocket 테스트 페이지 사용 흐름

1. 게스트 로그인 실행
2. 방 목록 조회 또는 테스트 데이터 생성
3. 방 생성 통합 실행
   - `POST /api/v1/rooms` 실행
   - 생성 응답에서 `roomId` 추출
   - `/ws` 연결 후 `/user/queue/errors`, `/user/queue/pong`, `/topic/room/{roomId}` 구독
4. 방 참가 통합 실행
   - `POST /api/v1/rooms/{roomId}/join` 실행
   - `/user/queue/errors`, `/user/queue/pong`, `/topic/room/{roomId}` 구독
   - `SEND /app/room/join` (구독 중인 room topic으로 `PLAYER_JOIN` 수신)
5. 게임 시작 및 로딩 완료 전송
   - `SEND /app/room/start`로 `GAME_START` 수신
   - `GAME_START` 수신 후 자동 구독된 game topic에서 `SEND /app/game/load`
6. `READY/UNREADY`, `정답 제출`, `LEAVE + DISCONNECT` 테스트
7. `PING 전송`으로 pong 응답 확인

정확한 payload, 에러 코드, 이벤트 계약은 `docs/STOMP_MESSAGE_SPEC.md`를 기준으로 확인하세요.

## Docker 실행 기본값

- `Dockerfile` 기본 JVM 옵션:
  - 타임존: `Asia/Seoul` (`TZ`, `-Duser.timezone`)
  - 애플리케이션 로그 파일: `/var/log/sigkill/app.log` (`LOGGING_FILE_NAME`)
  - 힙: `-Xms512m -Xmx1024m`
  - GC: `G1GC` + `MaxGCPauseMillis=200`
  - OOM: `-XX:+HeapDumpOnOutOfMemoryError`, `-XX:HeapDumpPath=/app/logs`, `-XX:+ExitOnOutOfMemoryError`
  - 권장 컨테이너 제한(개발 서버 4GB, 추후 RDB/Redis 공존 고려): `--memory=1536m --memory-swap=1536m`

테스트용으로 앱과 Redis를 같이 띄울 때는 [`docker-compose.yml`](./docker-compose.yml) 사용을 기준으로 합니다.

```bash
./gradlew clean build
SPRING_PROFILES_ACTIVE=dev docker compose up --build
```

Redis는 비밀번호 없이 `redis:7-alpine`로 올라오고, 앱 컨테이너는 내부 DNS 이름 `redis:6379`로 접속합니다.

단일 앱 컨테이너만 수동 실행하려면 여전히 아래처럼 별도 Redis를 준비한 뒤 `docker run`을 사용할 수 있습니다.

```bash
docker build -t sigkill-server .

docker run -d --name sigkill-server -p 8080:8080 \
  --memory=1536m --memory-swap=1536m \
  -e TZ=Asia/Seoul \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e LOGGING_FILE_NAME=/var/log/sigkill/app.log \
  -e JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Seoul -Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs -XX:+ExitOnOutOfMemoryError" \
  -v "$(pwd)/logs:/app/logs" \
  -v "$(pwd)/runtime-logs:/var/log/sigkill" \
  sigkill-server
```

## 자동배포(GitHub Actions) 메모

- 워크플로 파일:
  - dev: [`../.github/workflows/deploy-sigkill-server-develop.yml`](../.github/workflows/deploy-sigkill-server-develop.yml)
  - prod: [`../.github/workflows/deploy-sigkill-server-main.yml`](../.github/workflows/deploy-sigkill-server-main.yml)
- 배포용 compose 폴더:
  - dev: [`./deploy/dev/docker-compose.yml`](./deploy/dev/docker-compose.yml)
  - prod app: [`./deploy/prod/app/docker-compose.yml`](./deploy/prod/app/docker-compose.yml)
- Docker 이미지 태그:
  - 공통: `sha-<git sha>`
  - `develop` 브랜치: `develop`
  - `main` 브랜치: `latest`

### `develop` 브랜치

- 대상 인스턴스: 테스트용 dev 인스턴스
- 업로드 파일: [`./deploy/dev/docker-compose.yml`](./deploy/dev/docker-compose.yml)
- 배포 방식: `SPRING_PROFILES_ACTIVE=dev`를 명시한 뒤 앱 + Redis를 같은 인스턴스에서 `docker compose up -d --remove-orphans`
- 기동 서비스:
  - `sigkill-server`
  - `sigkill-redis`

필요 GitHub Secrets:

- `OCI_DEV_HOST`
- `OCI_USER`
- `OCI_SSH_KEY`
- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

### `main` 브랜치

- 대상 인스턴스:
  - Spring Boot 앱: `was-1`
  - Redis 세션: DB/Redis 서버에서 운영 중인 기존 `redis-session` 컨테이너
- 업로드 파일: [`./deploy/prod/app/docker-compose.yml`](./deploy/prod/app/docker-compose.yml)
- 배포 방식:
  - 앱은 `was-1`에서 단독 기동
  - Redis 컨테이너는 GitHub Actions가 건드리지 않고, 앱이 기존 `redis-session:6379`에 연결
  - 앱 컨테이너는 원격 `docker login` 후 `SPRING_PROFILES_ACTIVE=prod docker compose up --pull always --remove-orphans`로 갱신

필요 GitHub Secrets:

- `PROD_WAS_HOST`
- `PROD_REDIS_HOST`
  - DB/Redis 서버의 private IP
- `OCI_USER`
- `OCI_SSH_KEY`
- `REDIS_PASSWORD`
  - 현재 운영 Redis 세션 비밀번호
- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

prod 앱 컨테이너는 `SPRING_DATA_REDIS_HOST=<DB/Redis private IP>`, `SPRING_DATA_REDIS_PORT=6379`, `SPRING_DATA_REDIS_PASSWORD=<REDIS_PASSWORD>`로 기존 Redis 세션 인스턴스에 연결합니다.
