import { test, expect } from "@playwright/test";
import { ROUTE_PATHS } from "@/routes/paths";

test.describe("인증/인가", () => {
  test.describe("비회원 로그인", () => {
    test("랜딩 페이지에서 게임 시작 버튼을 클릭하면 세션이 쿠키에 저장되고 방 목록 페이지로 이동한다", async ({
      page,
    }) => {
      // Arrange
      await page.goto(ROUTE_PATHS.HOME);

      // Act
      await page.getByRole("button", { name: "Game Start" }).click();

      // Assert
      await expect(page).toHaveURL(ROUTE_PATHS.ROOM_LIST);

      // 세션 쿠키 존재 확인
      const cookies = await page.context().cookies();
      expect(cookies.some(c => c.name === "JSESSIONID")).toBe(true);

      // 3초간 랜딩으로 이동하지 않음 확인
      await expect
        .poll(() => new URL(page.url()).pathname, { timeout: 3000 })
        .toBe(ROUTE_PATHS.ROOM_LIST);
    });
  });
});
