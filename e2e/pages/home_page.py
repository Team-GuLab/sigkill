from playwright.sync_api import Page, Locator, expect

from pages.base_page import BasePage


class HomePage(BasePage):
    """서비스 진입 페이지 (/)"""

    URL = "/"

    def __init__(self, page: Page) -> None:
        super().__init__(page)

    # --- 로케이터 ---

    @property
    def title_heading(self) -> Locator:
        """예전 랜딩의 'SIGKILL' 제목 헤딩"""
        return self.page.get_by_role("heading", name="SIGKILL", level=1)

    @property
    def rooms_heading(self) -> Locator:
        """현재 메인 화면의 '방 목록' 제목 헤딩"""
        return self.page.get_by_role("heading", name="방 목록", level=1)

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
        """예전 랜딩은 버튼 클릭, 현재 메인은 이미 방 목록이면 그대로 진행"""
        entry_point = self.game_start_button.or_(self.rooms_heading)
        expect(entry_point.first).to_be_visible()

        if self.game_start_button.count() > 0 and self.game_start_button.first.is_visible():
            self.game_start_button.first.click()
            return

        expect(self.rooms_heading).to_be_visible()
