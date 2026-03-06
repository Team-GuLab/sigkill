from playwright.sync_api import Page, Locator, expect

from pages.base_page import BasePage


class GameResultPage(BasePage):
    """게임 결과 모달 화면"""

    def __init__(self, page: Page) -> None:
        super().__init__(page)

    @property
    def modal(self) -> Locator:
        """'경기 결과' 헤딩을 포함하는 결과 모달 루트"""
        result_heading = self.page.get_by_role("heading", name="경기 결과", level=2)
        return self.page.locator("main").filter(has=result_heading).first

    @property
    def heading(self) -> Locator:
        """'경기 결과' 제목"""
        return self.modal.get_by_role("heading", name="경기 결과", level=2)

    @property
    def confirm_button(self) -> Locator:
        """결과 모달 확인 버튼"""
        return self.modal.get_by_role("button", name="확인")

    @property
    def score_header(self) -> Locator:
        """점수 컬럼 헤더"""
        return self.modal.get_by_text("점수", exact=True)

    def player_name(self, nickname: str) -> Locator:
        """결과 모달 내 플레이어 닉네임"""
        return self.modal.get_by_text(nickname, exact=True)

    def wait_until_visible(self, timeout: int = 90_000) -> None:
        """결과 모달 표시까지 대기"""
        expect(self.heading).to_be_visible(timeout=timeout)

    def click_confirm(self) -> None:
        """확인 버튼 클릭"""
        self.confirm_button.click()
