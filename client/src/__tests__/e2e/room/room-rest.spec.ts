import { test, expect } from "@playwright/test";
import { ROUTE_PATHS } from "@/routes/paths";

test.describe("방", () => {
  test.describe("생성하기", () => {
    test.beforeEach(async ({ page }) => {
      // 랜딩페이지에서 Game Start 클릭으로 로그인 후 방 목록 진입
      await page.goto(ROUTE_PATHS.HOME);
      await page.getByRole("button", { name: "Game Start" }).click();
      await expect(page).toHaveURL(ROUTE_PATHS.ROOM_LIST);

      await page.getByRole("button", { name: "방 생성" }).click();
    });

    test("방 제목을 20자 초과하여 입력하면 '방 제목의 길이는 최대 20자 입니다' 토스트가 뜨며 페이지 이동이 일어나지 않는다", async ({
      page,
    }) => {
      await page.getByLabel("방 제목").fill("a".repeat(21));
      await page.getByRole("button", { name: "생성하기" }).click();

      // 토스트 노출 확인
      await expect(
        page.getByText("방 제목의 길이는 최대 20자 입니다"),
      ).toBeVisible();

      // 페이지 이동 없음 확인
      await expect(page).toHaveURL(ROUTE_PATHS.ROOM_LIST);
    });

    test("방을 생성하면 다른 사용자에게 생성한 방 정보가 보인다", async ({
      page,
      browser,
    }) => {
      // 사용자 B: 새 브라우저 컨텍스트로 로그인
      const contextB = await browser.newContext();
      const pageB = await contextB.newPage();

      // Act
      await page.getByLabel("방 제목").fill("SIGKILL");
      await page.getByRole("button", { name: "생성하기" }).click();

      // Assert
      await expect(pageB.getByText("SIGKILL").first()).toBeVisible({
        timeout: 10000,
      });
    });
  });
});
