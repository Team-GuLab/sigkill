export async function enableMocking() {
  if (import.meta.env.VITE_MOCKING !== "true") {
    return;
  }

  if (import.meta.env.MODE !== "development") {
    return;
  }

  const { worker } = await import("./browser");

  // 서비스 워커가 준비되어 요청을 가로칠 준비가 완료되면,
  // worker.start()는 resolve된 Promise를 반환함.
  return worker.start();
}
