import { test, expect } from "@playwright/test";
import { ROUTE_PATHS, ROUTE_GENERATORS } from "@/routes/paths";

test.describe("방", () => {
  test.describe("생성하기", () => {
    test.beforeEach(async ({ page }) => {
      // 랜딩페이지에서 Game Start 클릭으로 로그인 후 방 목록 진입
      await page.goto(ROUTE_PATHS.HOME);
      await page.getByRole("button", { name: "Game Start" }).click();
      await expect(page).toHaveURL(ROUTE_PATHS.ROOM_LIST);
    });

    test("방 제목을 20자 초과하여 입력하면 '방 제목의 길이는 최대 20자 입니다' 토스트가 뜨며 페이지 이동이 일어나지 않는다", async ({
      page,
    }) => {
      await page.getByRole("button", { name: "방 생성" }).click();
      await page.getByLabel("방 제목").fill("a".repeat(21));
      await page.getByRole("button", { name: "생성하기" }).click();

      // 토스트 노출 확인
      await expect(
        page.getByText("방 제목의 길이는 최대 20자 입니다"),
      ).toBeVisible();

      // 페이지 이동 없음 확인
      await expect(page).toHaveURL(ROUTE_PATHS.ROOM_LIST);
    });

    test("방 생성 후 대기방으로 이동하면 웹소켓 연결이 된다", async ({
      page,
    }) => {
      const wsPromise = page.waitForEvent("websocket");

      const createRoomResponsePromise = page.waitForResponse(
        res =>
          res.url().includes("/api/v1/rooms") &&
          res.request().method() === "POST",
      );

      await page.getByRole("button", { name: "방 생성" }).click();
      await page.getByLabel("방 제목").fill("테스트 방");
      await page.getByRole("button", { name: "생성하기" }).click();

      const response = await createRoomResponsePromise;
      const body = await response.json();
      const roomId = body.result.room.roomId;

      await expect(page).toHaveURL(ROUTE_GENERATORS.WAITING_ROOM(roomId));

      const ws = await wsPromise;
      expect(ws.url()).toContain("ws");
    });
  });
});
