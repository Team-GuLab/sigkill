from playwright.sync_api import Page


class BasePage:
    BASE_URL = "https://sigkill-quiz.kr"

    def __init__(self, page: Page) -> None:
        self.page = page

    def navigate(self, path: str = "") -> None:
        self.page.goto(f"{self.BASE_URL}{path}")
