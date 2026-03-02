import { test, expect } from "@playwright/test";
import { ROUTE_PATHS } from "@/routes/paths";
import { setupRoom, expectToBeVisibleInToast, uniqueRoomTitle } from "../utils";

test.describe("방", () => {
  test.describe("생성하기", () => {
    test.beforeEach(async ({ page }) => {
      // 랜딩페이지에서 Game Start 클릭으로 로그인 후 방 목록 진입
      await page.goto(ROUTE_PATHS.HOME);
      await page.getByRole("button", { name: "Game Start" }).click();
      await expect(page).toHaveURL(ROUTE_PATHS.ROOM_LIST);

      await page.getByRole("button", { name: "방 생성" }).click();
    });

    test("방 제목을 20자 초과하여 입력하면 방 생성에 실패한다", async ({
      page,
    }) => {
      await page.getByLabel("방 제목").fill("a".repeat(21));
      await page.getByRole("button", { name: "생성하기" }).click();

      await expectToBeVisibleInToast(page, "방 제목의 길이는 최대 20자 입니다");

      // 페이지 이동 없음 확인
      await expect(page).toHaveURL(ROUTE_PATHS.ROOM_LIST);
    });

    test("방을 생성하면 다른 사용자에게 생성한 방 정보가 보인다", async ({
      page,
      browser,
    }) => {
      // 사용자 B: 새 브라우저 컨텍스트로 로그인 후 방 목록 진입
      const contextB = await browser.newContext();
      const pageB = await contextB.newPage();
      await pageB.goto(ROUTE_PATHS.HOME);
      await pageB.getByRole("button", { name: "Game Start" }).click();
      await expect(pageB).toHaveURL(ROUTE_PATHS.ROOM_LIST);

      // Act
      const roomTitle = uniqueRoomTitle("SIGKILL");
      await page.getByLabel("방 제목").fill(roomTitle);
      await page.getByRole("button", { name: "생성하기" }).click();

      // Assert
      await expectToBeVisibleInToast(pageB, roomTitle);

      await contextB.close();
    });
  });

  test.describe("입장하기", () => {
    test("이미 게임이 진행중인 방에는 입장할 수 없다", async ({
      page,
      browser,
    }) => {
      const { contextB, pageB, roomTitle } = await setupRoom(page, browser);
      // Arrange
      await pageB.getByRole("button", { name: "준비" }).click();
      await page.getByRole("button", { name: "게임 시작" }).click();

      // Act
      // 사용자 C: 새 브라우저 컨텍스트로 방 입장 시도
      const contextC = await browser.newContext();
      const pageC = await contextC.newPage();
      await pageC.goto(ROUTE_PATHS.HOME);
      await pageC.getByRole("button", { name: "Game Start" }).click();
      await expect(pageC).toHaveURL(ROUTE_PATHS.ROOM_LIST);

      await pageC.getByText(roomTitle).first().click();

      // Assert
      await expectToBeVisibleInToast(pageC, "이미 게임이 진행 중인 방입니다");

      await contextB.close();
      await contextC.close();
    });
  });
});
