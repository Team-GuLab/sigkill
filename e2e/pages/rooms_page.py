from playwright.sync_api import Page, Locator

from pages.base_page import BasePage


class CreateRoomDialog:
    """방 생성 다이얼로그"""

    def __init__(self, page: Page) -> None:
        self.page = page
        self.dialog = page.get_by_role("dialog", name="방 생성")

    # --- 로케이터 ---

    @property
    def heading(self) -> Locator:
        return self.dialog.get_by_role("heading", name="방 생성", level=2)

    @property
    def room_title_input(self) -> Locator:
        """방 제목 입력 필드"""
        return self.dialog.get_by_role("textbox", name="방 제목")

    @property
    def close_button(self) -> Locator:
        """닫기 버튼"""
        return self.dialog.get_by_role("button", name="닫기")

    @property
    def create_button(self) -> Locator:
        """생성하기 버튼"""
        return self.dialog.get_by_role("button", name="생성하기")

    # --- 액션 ---

    def fill_room_title(self, title: str) -> None:
        self.room_title_input.fill(title)

    def submit(self) -> None:
        """방 제목 입력 후 생성하기 버튼 클릭"""
        self.create_button.click()

    def close(self) -> None:
        """다이얼로그 닫기"""
        self.close_button.click()


class RoomsPage(BasePage):
    """방 목록 페이지 (/rooms)"""

    URL = "/rooms"

    def __init__(self, page: Page) -> None:
        super().__init__(page)
        self.create_room_dialog = CreateRoomDialog(page)

    # --- 로케이터 ---

    @property
    def heading(self) -> Locator:
        """'방 목록' 제목 헤딩"""
        return self.page.get_by_role("heading", name="방 목록", level=1)

    @property
    def room_list(self) -> Locator:
        """방 목록 리스트"""
        return self.page.get_by_role("list").first

    @property
    def room_items(self) -> Locator:
        """방 목록의 각 방 아이템"""
        return self.room_list.get_by_role("listitem")

    @property
    def create_room_button(self) -> Locator:
        """'방 생성' 버튼"""
        return self.page.get_by_role("button", name="방 생성")

    # 페이지네이션

    @property
    def pagination(self) -> Locator:
        return self.page.get_by_role("navigation", name="pagination")

    @property
    def prev_page_button(self) -> Locator:
        """이전 페이지 버튼"""
        return self.pagination.get_by_role("generic", name="Go to previous page")

    @property
    def next_page_button(self) -> Locator:
        """다음 페이지 버튼"""
        return self.pagination.get_by_role("generic", name="Go to next page")

    def page_number_button(self, number: int) -> Locator:
        """특정 페이지 번호 버튼"""
        return self.pagination.get_by_text(str(number))

    # 유저 정보

    @property
    def user_info(self) -> Locator:
        """유저 정보 영역"""
        return self.page.get_by_text("User ID:", exact=False)

    @property
    def user_nickname(self) -> Locator:
        """닉네임 텍스트 (User ID: 바로 위 형제 요소)"""
        return self.page.get_by_text("User ID:", exact=False).locator(
            "xpath=preceding-sibling::*[1]"
        )

    def get_nickname(self) -> str:
        """현재 로그인한 사용자의 닉네임 반환"""
        return self.user_nickname.inner_text()

    # --- 액션 ---

    def goto(self) -> None:
        """방 목록 페이지로 이동"""
        self.navigate(self.URL)

    def open_create_room_dialog(self) -> CreateRoomDialog:
        """방 생성 버튼 클릭 → 다이얼로그 열기"""
        self.create_room_button.click()
        return self.create_room_dialog

    def create_room(self, title: str) -> None:
        """방을 생성하고 다이얼로그를 제출"""
        dialog = self.open_create_room_dialog()
        dialog.fill_room_title(title)
        dialog.submit()

    def click_room_by_name(self, room_name: str) -> None:
        """방 이름으로 방 클릭"""
        self.page.get_by_text(room_name).click()

    def get_room_count(self) -> int:
        """현재 페이지의 방 개수 반환"""
        return self.room_items.count()
