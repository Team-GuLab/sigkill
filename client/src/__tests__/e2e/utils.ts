/**
 * 병렬 실행되는 테스트 간 방 이름 충돌을 방지하기 위한 유니크 방 제목 생성.
 * - 방 제목 최대 20자 제한을 준수
 *
 * @example
 * uniqueRoomTitle("SIGKILL") // "SIGKILL-A3F2X"
 */
export function uniqueRoomTitle(prefix: string): string {
  const suffix = Math.random().toString(36).slice(2, 7).toUpperCase();
  return `${prefix}${suffix}`;
}
