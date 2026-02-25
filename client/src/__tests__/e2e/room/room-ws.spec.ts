import { test, expect } from "@playwright/test";
import { ROUTE_PATHS, ROUTE_GENERATORS } from "@/routes/paths";

test.describe("방 웹소켓 및 STOMP", () => {
  test.beforeEach(async ({ page }) => {
    // 랜딩페이지에서 Game Start 클릭으로 로그인 후 방 목록 진입
    await page.goto(ROUTE_PATHS.HOME);
    await page.getByRole("button", { name: "Game Start" }).click();
    await expect(page).toHaveURL(ROUTE_PATHS.ROOM_LIST);

    await page.getByRole("button", { name: "방 생성" }).click();
  });

  test.describe("플레이어 입장", () => {
    test("사용자 B가 방에 입장하면 '입장했습니다.'를 포함한 토스트가 보인다", async ({
      page,
      browser,
    }) => {
      await page.getByLabel("방 제목").fill("SIGKILL");
      await page.getByRole("button", { name: "생성하기" }).click();

      // 사용자 B: 새 브라우저 컨텍스트로 로그인
      const contextB = await browser.newContext();
      const pageB = await contextB.newPage();

      await pageB.goto(ROUTE_PATHS.HOME);
      await pageB.getByRole("button", { name: "Game Start" }).click();
      await expect(pageB).toHaveURL(ROUTE_PATHS.ROOM_LIST);

      // 사용자 B의 방 목록에 사용자 A가 생성한 방이 나타날 때까지 대기 후 입장
      await expect(pageB.getByText("SIGKILL").first()).toBeVisible({
        timeout: 10000,
      });
      await pageB.getByText("SIGKILL").first().click();

      // 입장 토스트 확인
      await expect(pageB.getByText(/입장/)).toBeVisible();

      await contextB.close();
    });
  });
});
