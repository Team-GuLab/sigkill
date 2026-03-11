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
 * 토스트 메시지가 보이는지 확인
 * @param page - 페이지
 * @param text - 토스트 메시지 텍스트
 */
export async function expectToBeVisibleInToast(
  page: Page,
  text: string | RegExp,
): Promise<void> {
  await expect(
    page.getByRole("listitem").filter({ hasText: text }),
  ).toBeVisible({ timeout: 10000 });
}

/**
 * 두 명의 사용자가 대기방에 입장한 상태를 세팅
 * - 사용자 A(page): 방을 생성하고 입장
 * - 사용자 B(pageB): 별도 컨텍스트로 방에 입장
 */
export async function setupRoom(
  page: Page,
  browser: Browser,
): Promise<{
  contextB: BrowserContext;
  pageB: Page;
  roomTitle: string;
}> {
  const roomTitle = uniqueRoomTitle("SIGKILL");

  await page.goto(ROUTE_PATHS.LANDING);
  await page.getByRole("button", { name: "Game Start" }).click();
  await expect(page).toHaveURL(ROUTE_PATHS.ROOM_LIST);

  await page.getByRole("button", { name: "방 생성" }).click();
  await page.getByLabel("방 제목").fill(roomTitle);
  await page.getByRole("button", { name: "생성하기" }).click();

  // 사용자 B: 방 입장
  const contextB = await browser.newContext();
  const pageB = await contextB.newPage();

  await pageB.goto(ROUTE_PATHS.LANDING);
  await pageB.getByRole("button", { name: "Game Start" }).click();
  await expect(pageB).toHaveURL(ROUTE_PATHS.ROOM_LIST, {
    timeout: 10000,
  });

  await expect(pageB.getByText(roomTitle).first()).toBeVisible({
    timeout: 10000,
  });
  await pageB.getByText(roomTitle).first().click();

  // 사용자 B가 대기방에 완전히 입장했음을 보장
  await expect(pageB).toHaveURL(/\/waiting-room\//);
  await expect(pageB.getByRole("heading", { name: roomTitle })).toBeVisible({
    timeout: 10000,
  });

  return { contextB, pageB, roomTitle };
}
