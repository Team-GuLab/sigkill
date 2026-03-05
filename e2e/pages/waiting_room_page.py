from playwright.sync_api import Page, Locator

from pages.base_page import BasePage


class WaitingRoomPage(BasePage):
    """대기실 페이지 (/waiting-room/:roomId)"""

    URL_PATTERN = "/waiting-room/{room_id}"

    def __init__(self, page: Page) -> None:
        super().__init__(page)

    # --- 로케이터 ---

    @property
    def room_name_heading(self) -> Locator:
        """방 이름 헤딩 (h1)"""
        return self.page.get_by_role("heading", level=1)

    @property
    def room_id_text(self) -> Locator:
        """'Room ID: ...' 텍스트"""
        return self.page.get_by_text("Room ID:", exact=False)

    @property
    def participants_heading(self) -> Locator:
        """'참가자 (N/6명)' 헤딩 (h2)"""
        return self.page.get_by_role("heading", level=2)

    @property
    def participant_items(self) -> Locator:
        """참가자 슬롯 목록 (빈 자리 포함)"""
        return self.page.get_by_text("빈 자리").or_(
            self.page.locator("[class*='participant'], [class*='player']")
        )

    @property
    def participant_list(self) -> Locator:
        """참가자 목록 컨테이너 (heading의 형제 요소로 스코프 한정)"""
        return self.participants_heading.locator("xpath=following-sibling::*[1]")

    @property
    def empty_slots(self) -> Locator:
        """빈 자리 슬롯"""
        return self.page.get_by_text("빈 자리")

    def participant_by_name(self, name: str) -> Locator:
        """참가자 목록 내에서 닉네임으로 참가자 찾기 (토스트 알림과 혼동 방지)"""
        return self.participant_list.get_by_text(name, exact=True)

    @property
    def leave_button(self) -> Locator:
        """'나가기' 버튼"""
        return self.page.get_by_role("button", name="나가기")

    @property
    def ready_button(self) -> Locator:
        """'준비' 버튼"""
        return self.page.get_by_role("button", name="준비")

    @property
    def start_button(self) -> Locator:
        """'게임 시작' 버튼 (방장에게만 보임)"""
        return self.page.get_by_role("button", name="게임 시작")

    # --- 액션 ---

    def goto(self, room_id: int | str) -> None:
        """특정 방 대기실로 이동"""
        self.navigate(self.URL_PATTERN.format(room_id=room_id))

    def click_ready(self) -> None:
        """준비 버튼 클릭"""
        self.ready_button.click()

    def click_leave(self) -> None:
        """나가기 버튼 클릭 → 방 목록 페이지로 이동"""
        self.leave_button.click()

    def click_start(self) -> None:
        """게임 시작 버튼 클릭 (방장 전용)"""
        self.start_button.click()

    def get_room_name(self) -> str:
        """방 이름 텍스트 반환"""
        return self.room_name_heading.inner_text()

    def get_empty_slot_count(self) -> int:
        """빈 자리 개수 반환"""
        return self.empty_slots.count()
