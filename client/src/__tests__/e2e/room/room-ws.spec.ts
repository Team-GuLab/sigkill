import { test, expect, type BrowserContext, type Page } from "@playwright/test";
import { ROUTE_PATHS } from "@/routes/paths";
import { uniqueRoomTitle } from "../utils";

test.describe("대기방 관련 웹소켓(STOMP) 이벤트 처리", () => {
  let theOtherContext: BrowserContext;
  let theOtherPage: Page;
  let roomTitle: string;

  test.beforeEach(async ({ page, browser }) => {
    roomTitle = uniqueRoomTitle("SIGKILL");

    // 사용자 A
    await page.goto(ROUTE_PATHS.HOME);
    await page.getByRole("button", { name: "Game Start" }).click();
    await expect(page).toHaveURL(ROUTE_PATHS.ROOM_LIST);

    await page.getByRole("button", { name: "방 생성" }).click();
    await page.getByLabel("방 제목").fill(roomTitle);
    await page.getByRole("button", { name: "생성하기" }).click();

    // 사용자 B
    theOtherContext = await browser.newContext();
    theOtherPage = await theOtherContext.newPage();

    await theOtherPage.goto(ROUTE_PATHS.HOME);
    await theOtherPage.getByRole("button", { name: "Game Start" }).click();
    await expect(theOtherPage).toHaveURL(ROUTE_PATHS.ROOM_LIST);

    await expect(theOtherPage.getByText(roomTitle).first()).toBeVisible({
      timeout: 10000,
    });
    await theOtherPage.getByText(roomTitle).first().click();
  });

  test.afterEach(async () => {
    await theOtherContext?.close();
  });

  test.describe("플레이어 입장", () => {
    test("사용자 B가 방에 입장하면 입장 문구를 포함하는 토스트가 보인다", async ({
      page,
    }) => {
      await expect(page.getByRole("listitem").first()).toContainText(
        /.*님이 입장했습니다./,
      );
    });
  });

  test.describe("플레이어 퇴장", () => {
    test("사용자 B가 방에서 퇴장하면 퇴장 문구를 포함하는 토스트가 보인다", async ({
      page,
    }) => {
      await theOtherPage.getByRole("button", { name: "나가기" }).click();
      await expect(page.getByRole("listitem").first()).toContainText(
        /.*님이 퇴장했습니다./,
      );
    });
  });

  test.describe("방장 변경", () => {
    test("사용자 A가 나가면 방장 변경 문구를 포함하는 토스트가 보인다", async ({
      page,
    }) => {
      await page.getByRole("button", { name: "나가기" }).click();
      await expect(theOtherPage.getByRole("listitem").first()).toContainText(
        /방장이 .*님으로 변경되었습니다./,
      );
    });
  });

  test.describe("플레이어 준비 완료", () => {
    test("사용자 B가 준비하면 준비 문구를 포함하는 토스트가 보인다", async ({
      page,
    }) => {
      await theOtherPage.getByRole("button", { name: "준비" }).click();
      expect(page.getByRole("listitem").first()).toContainText(
        /.*님이 준비했습니다./,
      );
    });
  });

  test.describe("플레이어 준비 취소", () => {
    test("사용자 B가 준비 취소하면 준비 취소 문구를 포함하는 토스트가 보인다", async ({
      page,
    }) => {
      await theOtherPage.getByRole("button", { name: "준비" }).click();
      await theOtherPage.getByRole("button", { name: "준비 취소" }).click();
      await expect(page.getByRole("listitem").first()).toContainText(
        /.*님이 준비 취소했습니다./,
      );
    });
  });
});
