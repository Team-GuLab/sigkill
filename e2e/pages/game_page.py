import re

from playwright.sync_api import Page, Locator, expect

from pages.base_page import BasePage


class GamePage(BasePage):
    """게임 진행/종료 화면"""

    def __init__(self, page: Page) -> None:
        super().__init__(page)

    # --- 로케이터 ---

    @property
    def question_progress_text(self) -> Locator:
        """'1/5' 형태의 문제 진행도 텍스트"""
        return self.page.get_by_text(re.compile(r"^\d+/\d+$")).first

    @property
    def timer_text(self) -> Locator:
        """'3.2s' 형태의 남은 시간 텍스트"""
        return self.page.get_by_text(re.compile(r"^\d+(\.\d+)?s$")).first

    @property
    def answer_buttons(self) -> Locator:
        """'1. ...' ~ '4. ...' 답안 버튼"""
        return self.page.get_by_role("button", name=re.compile(r"^\d+\.\s"))

    @property
    def game_end_notice(self) -> Locator:
        """게임 종료 안내 배너 텍스트"""
        return self.page.get_by_text("게임이 종료되었어요", exact=False)

    @property
    def result_heading(self) -> Locator:
        """결과 모달 제목"""
        return self.page.get_by_role("heading", name="경기 결과", level=2)

    @property
    def result_confirm_button(self) -> Locator:
        """결과 모달의 확인 버튼"""
        return self.page.get_by_role("button", name="확인")

    @property
    def all_eliminated_alert(self) -> Locator:
        """전원 탈락 알림"""
        return self.page.get_by_role("alert").get_by_text("전원탈락")

    def answer_button(self, choice_number: int) -> Locator:
        """번호 기반 답안 버튼 (1~4)"""
        return self.page.get_by_role(
            "button", name=re.compile(rf"^{choice_number}\.\s")
        )

    # --- 액션 ---

    def wait_until_loaded(self, timeout: int = 10_000) -> None:
        """게임 진행 또는 종료 화면이 보일 때까지 대기"""
        visible_state = (
            self.question_progress_text.or_(self.game_end_notice).or_(self.result_heading)
        )
        expect(visible_state.first).to_be_visible(timeout=timeout)

    def click_answer(self, choice_number: int) -> None:
        """답안 번호를 선택"""
        self.answer_button(choice_number).click()

    def wait_until_result_visible(self, timeout: int = 15_000) -> None:
        """결과 모달이 뜰 때까지 대기"""
        expect(self.result_heading).to_be_visible(timeout=timeout)

    def click_result_confirm(self) -> None:
        """결과 모달 확인 버튼 클릭"""
        self.result_confirm_button.click()

    def get_answer_count(self) -> int:
        """답안 버튼 개수 반환"""
        return self.answer_buttons.count()

    def is_answer_disabled(self, choice_number: int) -> bool:
        """특정 답안 버튼 비활성화 여부"""
        return self.answer_button(choice_number).is_disabled()

