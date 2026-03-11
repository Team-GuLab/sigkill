import uuid

from playwright.sync_api import Page, expect

from pages.game_page import GamePage
from pages.game_result_page import GameResultPage
from pages.rooms_page import RoomsPage
from pages.home_page import HomePage
from pages.waiting_room_page import WaitingRoomPage


def _get_random_title():
    return f"테스트방_{uuid.uuid4().hex[:6]}"  # '테스트방_a3f9c1'


def test_1_게임_시작_버튼을_눌러_방_목록_화면으로_이동한다(page: Page):
    base_page = HomePage(page)
    rooms_page = RoomsPage(page)
    base_page.navigate()
    base_page.click_game_start()

    expect(rooms_page.heading).to_be_visible()
    expect(rooms_page.user_info).to_be_visible()


def test_2_방을_생성한다(make_page):
    page1 = make_page()
    base_page = HomePage(page1)
    rooms_page = RoomsPage(page1)
    waiting_page = WaitingRoomPage(page1)
    base_page.navigate()
    base_page.click_game_start()
    title = _get_random_title()
    rooms_page.create_room(title)
    expect(waiting_page.room_name_heading).to_be_visible()


def test_3_방에_참가한다(make_page):
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


def test_4_게임을_시작한다(make_page):
    page1 = make_page()
    page2 = make_page()
    base_page1 = HomePage(page1)
    rooms_page1 = RoomsPage(page1)
    waiting_page1 = WaitingRoomPage(page1)
    game_page1 = GamePage(page1)
    base_page1.navigate()
    base_page1.click_game_start()

    base_page2 = HomePage(page2)
    rooms_page2 = RoomsPage(page2)
    waiting_page2 = WaitingRoomPage(page2)
    game_page2 = GamePage(page2)

    base_page2.navigate()
    base_page2.click_game_start()

    title = _get_random_title()
    rooms_page1.create_room(title)
    expect(waiting_page1.room_name_heading).to_be_visible()

    rooms_page2.click_room_by_name(title)
    expect(waiting_page2.room_name_heading).to_be_visible()
    waiting_page2.click_ready()
    waiting_page1.click_start()

    game_page1.wait_until_loaded()
    game_page2.wait_until_loaded()
    expect(game_page1.answer_buttons).to_have_count(4)
    expect(game_page2.answer_buttons).to_have_count(4)


def test_5_게임을_진행하고_결과를_확인한다(make_page):
    page1 = make_page()
    page2 = make_page()
    base_page1 = HomePage(page1)
    rooms_page1 = RoomsPage(page1)
    waiting_page1 = WaitingRoomPage(page1)
    game_page1 = GamePage(page1)
    result_page1 = GameResultPage(page1)
    base_page1.navigate()
    base_page1.click_game_start()

    base_page2 = HomePage(page2)
    rooms_page2 = RoomsPage(page2)
    waiting_page2 = WaitingRoomPage(page2)
    game_page2 = GamePage(page2)

    base_page2.navigate()
    base_page2.click_game_start()

    title = _get_random_title()
    name1 = rooms_page1.get_nickname()
    name2 = rooms_page2.get_nickname()
    rooms_page1.create_room(title)
    expect(waiting_page1.room_name_heading).to_be_visible()

    rooms_page2.click_room_by_name(title)
    expect(waiting_page2.room_name_heading).to_be_visible()
    waiting_page2.click_ready()
    waiting_page1.click_start()

    game_page1.wait_until_loaded()
    game_page2.wait_until_loaded()
    expect(game_page1.answer_buttons).to_have_count(4)
    expect(game_page2.answer_buttons).to_have_count(4)

    game_page1.click_answer(1)
    result_page1.wait_until_visible(timeout=90_000)
    expect(result_page1.score_header).to_be_visible()
    expect(result_page1.confirm_button).to_be_visible()
    expect(result_page1.player_name(name1)).to_be_visible()
    expect(result_page1.player_name(name2)).to_be_visible()
