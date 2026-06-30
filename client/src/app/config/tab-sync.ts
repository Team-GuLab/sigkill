const CHANNEL_NAME = "sigkill-tab";
const TAB_ID_KEY = "sigkill-tab-id";

type TabMessage =
  | { type: "QUERY"; fromTabId: string }
  | { type: "RESPONSE"; fromTabId: string };

function getTabId(): string {
  let id = sessionStorage.getItem(TAB_ID_KEY);
  if (!id) {
    id = crypto.randomUUID();
    sessionStorage.setItem(TAB_ID_KEY, id);
  }
  return id;
}

function createTabSync() {
  const channel = new BroadcastChannel(CHANNEL_NAME);
  const tabId = getTabId();
  let responding = false;

  channel.onmessage = (event: MessageEvent<TabMessage>) => {
    const msg = event.data;
    if (msg.type === "QUERY" && msg.fromTabId !== tabId && responding) {
      channel.postMessage({ type: "RESPONSE", fromTabId: tabId });
    }
  };

  return {
    tabId,

    // 세션 활성 상태 제어: 세션 진입 시 true, 세션 이탈/중복 차단 시 false
    setResponding(value: boolean) {
      responding = value;
    },

    queryOtherTabs(timeout = 300): Promise<boolean> {
      return new Promise(resolve => {
        let resolved = false;

        const handler = (event: MessageEvent<TabMessage>) => {
          if (
            event.data.type === "RESPONSE" &&
            event.data.fromTabId !== tabId
          ) {
            if (!resolved) {
              resolved = true;
              channel.removeEventListener("message", handler);
              resolve(true);
            }
          }
        };

        channel.addEventListener("message", handler);
        channel.postMessage({ type: "QUERY", fromTabId: tabId });

        setTimeout(() => {
          if (!resolved) {
            channel.removeEventListener("message", handler);
            resolve(false);
          }
        }, timeout);
      });
    },

    destroy() {
      channel.close();
    },
  };
}

let instance: ReturnType<typeof createTabSync> | null = null;

export function getTabSync() {
  if (!instance) {
    instance = createTabSync();
  }
  return instance;
}
