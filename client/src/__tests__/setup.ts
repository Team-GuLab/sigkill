import "@testing-library/jest-dom";

beforeAll(() => {});

afterEach(() => {
  vi.clearAllMocks();
});

afterAll(() => {
  vi.resetAllMocks();
});

// stomp 관련 모듈 모킹
vi.mock("@/app/config/web-socket-client", () => ({
  connectWebSocket: vi.fn(),
  publishMessage: vi.fn(),
  disconnectWebSocket: vi.fn(),
}));

vi.mock("@/app/config/stomp-subscribe-manager", () => ({
  subscribeManager: vi.fn(),
}));
