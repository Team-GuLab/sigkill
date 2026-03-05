import pytest
from playwright.sync_api import Browser, Page


@pytest.fixture
def make_page(browser: Browser):
    """원하는 수만큼 독립된 브라우저 세션(Page)을 생성하는 팩토리 픽스처"""
    contexts = []

    def _make() -> Page:
        ctx = browser.new_context()
        contexts.append(ctx)
        return ctx.new_page()

    yield _make

    for ctx in contexts:
        ctx.close()
