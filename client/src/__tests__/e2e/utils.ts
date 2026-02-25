import {
  expect,
  type Browser,
  type BrowserContext,
  type Page,
} from "@playwright/test";
import { ROUTE_PATHS } from "@/routes/paths";

/**
 * 병렬 실행되는 테스트 간 방 이름 충돌을 방지하기 위한 유니크 방 제목 생성
 * - 방 제목 최대 20자 제한을 준수
 */
export function uniqueRoomTitle(prefix: string): string {
  const suffix = Math.random().toString(36).slice(2, 7).toUpperCase();
  return `${prefix}${suffix}`;
}

/**
 * 두 명의 사용자가 대기방에 입장한 상태를 세팅
 * - 사용자 A(page): 방을 생성하고 입장
 * - 사용자 B(theOtherPage): 별도 컨텍스트로 방에 입장
 */
export async function setupRoom(
  page: Page,
  browser: Browser,
): Promise<{ theOtherContext: BrowserContext; theOtherPage: Page }> {
  const roomTitle = uniqueRoomTitle("SIGKILL");

  await page.goto(ROUTE_PATHS.HOME);
  await page.getByRole("button", { name: "Game Start" }).click();
  await expect(page).toHaveURL(ROUTE_PATHS.ROOM_LIST);

  await page.getByRole("button", { name: "방 생성" }).click();
  await page.getByLabel("방 제목").fill(roomTitle);
  await page.getByRole("button", { name: "생성하기" }).click();

  const theOtherContext = await browser.newContext();
  const theOtherPage = await theOtherContext.newPage();

  await theOtherPage.goto(ROUTE_PATHS.HOME);
  await theOtherPage.getByRole("button", { name: "Game Start" }).click();
  await expect(theOtherPage).toHaveURL(ROUTE_PATHS.ROOM_LIST);

  await expect(theOtherPage.getByText(roomTitle).first()).toBeVisible({
    timeout: 10000,
  });
  await theOtherPage.getByText(roomTitle).first().click();

  return { theOtherContext, theOtherPage };
}
