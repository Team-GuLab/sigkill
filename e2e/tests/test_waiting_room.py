import re
import uuid

from playwright.sync_api import Page, expect

from pages.rooms_page import RoomsPage
from pages.home_page import HomePage
from pages.waiting_room_page import WaitingRoomPage

ROOM_TITLE = "멀티플레이어 테스트 방"


def _get_random_title():
    return f"테스트방_{uuid.uuid4().hex[:6]}"  # '테스트방_a3f9c1'


def test_게임_시작_버튼을_눌러_방_목록_화면으로_이동한다(page: Page):
    base_page = HomePage(page)
    rooms_page = RoomsPage(page)
    base_page.navigate()
    base_page.click_game_start()

    expect(rooms_page.heading).to_be_visible()
    expect(rooms_page.user_info).to_be_visible()


def test_방을_생성한다(make_page):
    page1 = make_page()
    base_page = HomePage(page1)
    rooms_page = RoomsPage(page1)
    base_page.navigate()
    base_page.click_game_start()
    title = _get_random_title()
    rooms_page.create_room(title)


def test_방에_참가한다(make_page):
    page1 = make_page()
    page2 = make_page()
    base_page1 = HomePage(page1)
    rooms_page1 = RoomsPage(page1)
    waiting_page1 = WaitingRoomPage(page1)
    base_page1.navigate()
    base_page1.click_game_start()

    base_page2 = HomePage(page2)
    rooms_page2 = RoomsPage(page2)
    waiting_page2 = WaitingRoomPage(page2)

    base_page2.navigate()
    base_page2.click_game_start()

    title = _get_random_title()
    name2 = rooms_page2.get_nickname()
    rooms_page1.create_room(title)
    expect(waiting_page1.room_name_heading).to_be_visible()

    rooms_page2.click_room_by_name(title)
    expect(waiting_page2.room_name_heading).to_be_visible()

    expect(waiting_page1.participant_by_name(name2)).to_be_visible()
    expect(waiting_page1.empty_slots).to_have_count(4)
