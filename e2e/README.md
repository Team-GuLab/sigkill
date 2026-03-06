# SIGKILL E2E 테스트 가이드

이 폴더는 SIGKILL 서비스의 핵심 사용자 흐름을 Playwright + Pytest로 검증하는 E2E 테스트 프로젝트입니다.

## 1) 폴더 구조

```text
e2e/
├─ pages/                  # Page Object Model (POM)
│  ├─ home_page.py
│  ├─ rooms_page.py
│  ├─ waiting_room_page.py
│  ├─ game_page.py
│  └─ game_result_page.py
├─ tests/
│  └─ test_game_flow.py # 주요 E2E 시나리오
├─ conftest.py             # pytest fixture(page, make_page)
├─ requirements.txt
├─ environment.yml
└─ TESTING_NOTES.md
```

## 2) 무엇을 검증하나요?

- 홈에서 방 목록 진입
- 방 생성 / 방 참가
- 게임 시작 후 문제 화면 진입
- 게임 진행 후 결과 화면(경기 결과) 확인

즉, 사용자 관점의 핵심 여정(매칭 -> 게임 시작 -> 플레이 -> 결과)을 검증합니다.

## 3) 설치 방법 (conda)

```bash
conda env create -f environment.yml
conda activate sigkill-e2e
python -m playwright install chromium
```

## 4) 테스트 실행 방법

### 전체 실행

```bash
pytest
```

### 전체 UI 포함 실행

```bash
pytest --headed
```

### 특정 테스트만 실행

```bash
pytest -k "게임을_시작한다"
pytest -k "게임을_진행하고_결과를_확인한다"
```

## 5) 테스트 작성 규칙 요약

- POM은 `pages/` 하위에 추가하고, 테스트는 `tests/`에서만 시나리오를 조합합니다.
- 여러 사용자가 필요한 경우 `make_page` fixture로 독립 세션 페이지를 생성합니다.
- UI 동기화는 `expect(...)` 기반으로 처리하고, 임의의 sleep 사용을 지양합니다.
- 추가 규칙은 `TESTING_NOTES.md`를 먼저 확인합니다.

## 6) 환경/동작 참고사항

- 기본 대상 URL은 `https://sigkill-quiz.kr` 이며, `E2E_BASE_URL` 환경변수로 오버라이드할 수 있습니다.
- 예시: `E2E_BASE_URL=https://staging.sigkill-quiz.kr pytest -q`
- 결과 화면 검증처럼 시간이 걸리는 시나리오는 timeout을 충분히 크게 설정해야 합니다.
