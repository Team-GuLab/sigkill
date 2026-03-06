import os
import time

from playwright.sync_api import Page


class BasePage:
    BASE_URL = os.environ.get("E2E_BASE_URL", "https://sigkill-quiz.kr").rstrip("/")

    def __init__(self, page: Page) -> None:
        self.page = page

    def navigate(self, path: str = "", retries: int = 3, delay: float = 1.0) -> None:
        url = f"{self.BASE_URL}{path}"
        for attempt in range(retries):
            try:
                self.page.goto(url)
                return
            except Exception:
                if attempt == retries - 1:
                    raise
                time.sleep(delay)
