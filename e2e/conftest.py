import pytest
from playwright.sync_api import Browser, BrowserContext, Page


@pytest.fixture
def page(context: BrowserContext) -> Page:
    """기본 page 픽스처 오버라이드 - WebSocket 연결 정리 보장"""
    page = context.new_page()
    yield page
    page.close()


@pytest.fixture
def make_page(browser: Browser):
    """원하는 수만큼 독립된 브라우저 세션(Page)을 생성하는 팩토리 픽스처"""
    pages = []
    contexts = []

    def _make() -> Page:
        ctx = browser.new_context()
        contexts.append(ctx)
        page = ctx.new_page()
        pages.append(page)
        return page

    yield _make

    for page in pages:
        page.close()

    for ctx in contexts:
        ctx.close()
