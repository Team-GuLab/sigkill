from playwright.sync_api import Page, Locator

from pages.base_page import BasePage


class HomePage(BasePage):
    """메인 랜딩 페이지 (/)"""

    URL = "/"

    def __init__(self, page: Page) -> None:
        super().__init__(page)

    # --- 로케이터 ---

    @property
    def title_heading(self) -> Locator:
        """'SIGKILL' 제목 헤딩"""
        return self.page.get_by_role("heading", name="SIGKILL", level=1)

    @property
    def subtitle(self) -> Locator:
        """'개발자들을 위한 실시간 퀴즈 배틀' 부제목"""
        return self.page.get_by_text("개발자들을 위한 실시간 퀴즈 배틀")

    @property
    def game_start_button(self) -> Locator:
        """'Game Start' 버튼"""
        return self.page.get_by_role("button", name="Game Start")

    # --- 액션 ---

    def goto(self) -> None:
        """메인 페이지로 이동"""
        self.navigate(self.URL)

    def click_game_start(self) -> None:
        """Game Start 버튼 클릭 → 방 목록 페이지로 이동"""
        self.game_start_button.click()
