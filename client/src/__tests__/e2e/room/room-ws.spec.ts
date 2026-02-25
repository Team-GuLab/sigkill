import { test, expect, type BrowserContext, type Page } from "@playwright/test";
import { setupRoom } from "../utils";

test.describe("대기방 관련 웹소켓(STOMP) 이벤트 처리", () => {
  let theOtherContext: BrowserContext;
  let theOtherPage: Page;

  test.afterEach(async ({ page }) => {
    // 두 사용자 모두 정리
    await page
      .getByRole("button", { name: "나가기" })
      .click({ timeout: 3000 })
      .catch(() => {});
    await theOtherContext?.close();
  });

  test.describe("플레이어 입장", () => {
    test("사용자 B가 방에 입장하면 입장 문구를 포함하는 토스트가 보인다", async ({
      page,
      browser,
    }) => {
      ({ theOtherContext, theOtherPage } = await setupRoom(page, browser));

      await expect(page.getByRole("listitem").first()).toContainText(
        /.*님이 입장했습니다./,
      );
    });
  });

  test.describe("플레이어 퇴장", () => {
    test("사용자 B가 방에서 퇴장하면 퇴장 문구를 포함하는 토스트가 보인다", async ({
      page,
      browser,
    }) => {
      ({ theOtherContext, theOtherPage } = await setupRoom(page, browser));

      await theOtherPage.getByRole("button", { name: "나가기" }).click();
      await expect(page.getByRole("listitem").first()).toContainText(
        /.*님이 퇴장했습니다./,
      );
    });
  });

  test.describe("방장 변경", () => {
    test("사용자 A가 나가면 방장 변경 문구를 포함하는 토스트가 보인다", async ({
      page,
      browser,
    }) => {
      ({ theOtherContext, theOtherPage } = await setupRoom(page, browser));

      await page.getByRole("button", { name: "나가기" }).click();
      await expect(theOtherPage.getByRole("listitem").first()).toContainText(
        /방장이 .*님으로 변경되었습니다./,
      );
    });
  });

  test.describe("플레이어 준비 완료", () => {
    test("사용자 B가 준비하면 준비 문구를 포함하는 토스트가 보인다", async ({
      page,
      browser,
    }) => {
      ({ theOtherContext, theOtherPage } = await setupRoom(page, browser));

      await theOtherPage.getByRole("button", { name: "준비" }).click();
      await expect(page.getByRole("listitem").first()).toContainText(
        /.*님이 준비했습니다./,
      );
    });
  });

  test.describe("플레이어 준비 취소", () => {
    test("사용자 B가 준비 취소하면 준비 취소 문구를 포함하는 토스트가 보인다", async ({
      page,
      browser,
    }) => {
      ({ theOtherContext, theOtherPage } = await setupRoom(page, browser));

      await theOtherPage.getByRole("button", { name: "준비" }).click();
      await theOtherPage.getByRole("button", { name: "준비 취소" }).click();
      await expect(page.getByRole("listitem").first()).toContainText(
        /.*님이 준비 취소했습니다./,
      );
    });
  });
});
