# sigkill-server

## 프론트엔드 개발자용 빠른 링크

- WebSocket 테스트 페이지: [http://localhost:8080/room-ws-test.html](http://localhost:8080/room-ws-test.html)
- Swagger UI: [http://localhost:8080/swagger-ui/index.html#/](http://localhost:8080/swagger-ui/index.html#/)

## Docker 실행 기본값

- `Dockerfile` 기본 JVM 옵션:
  - 타임존: `Asia/Seoul` (`TZ`, `-Duser.timezone`)
  - 힙: `-Xms512m -Xmx1024m`
  - GC: `G1GC` + `MaxGCPauseMillis=200`
  - OOM: `-XX:+HeapDumpOnOutOfMemoryError`, `-XX:HeapDumpPath=/app/logs`, `-XX:+ExitOnOutOfMemoryError`
  - 권장 컨테이너 제한(개발 서버 4GB, 추후 RDB/Redis 공존 고려): `--memory=1536m --memory-swap=1536m`

```bash
docker build -t sigkill-server .

docker run -d --name sigkill-server -p 8080:8080 \
  --memory=1536m --memory-swap=1536m \
  -e TZ=Asia/Seoul \
  -e JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Seoul -Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs -XX:+ExitOnOutOfMemoryError" \
  -v "$(pwd)/logs:/app/logs" \
  sigkill-server
```

## 자동배포(GitHub Actions) 메모리 제한

- 자동배포 시에도 컨테이너 메모리 제한을 동일하게 적용합니다.
- 파일: `.github/workflows/deploy-sigkill-server-develop.yml`
- `docker run` 옵션:
  - `--memory=1536m`
  - `--memory-swap=1536m`

```bash
docker run -d \
  --name sigkill-server \
  --restart unless-stopped \
  -p 8080:8080 \
  --memory=1536m \
  --memory-swap=1536m \
  -e SPRING_PROFILES_ACTIVE=dev \
  "${APP_IMAGE}"
```
