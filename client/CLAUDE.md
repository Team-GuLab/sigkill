# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
npm run dev        # Start Vite dev server
npm run build      # tsc + vite build
npm run lint       # ESLint
npm run test       # Vitest (unit/component tests)
npm run preview    # Preview production build
```

Playwright E2E tests are in `src/__tests__/*.spec.ts`. Run a single Playwright test:

```bash
npx playwright test src/__tests__/room.spec.ts
```

Run a single Vitest test file:

```bash
npx vitest run src/api/room/test/subscribe-room.test.ts
```

## Architecture

### Tech Stack

- **React 19** + TypeScript + Vite
- **React Router 7** for routing
- **Zustand 5** for global state (`src/store/`)
- **TanStack Query 5** for server state and caching
- **@stomp/stompjs** for WebSocket (STOMP protocol) real-time communication
- **Radix UI** + **Tailwind CSS 4** for UI
- **MSW 2** for API mocking in dev/test

Path alias: `@/` → `src/`

### Directory Layout

```
src/
├── api/           # API functions + WebSocket message handlers + types
├── app/config/    # WebSocket client singleton + STOMP subscribe manager
├── app/provider/  # TanStack Query provider
├── components/    # Feature components (room/, common/)
├── constants/     # App-wide constants
├── hooks/         # Custom hooks (data fetching, socket connection)
├── routes/        # Router config, pages, layouts, path constants
├── store/         # Zustand stores
├── ui/            # Low-level UI primitives (wrapping Radix UI)
├── widgets/       # Composite layout components (header, profile)
└── __mocks__/     # MSW handlers for testing
```

### Routing

Routes are defined in `src/routes/index.tsx` using `createBrowserRouter`. Path constants live in `src/routes/paths.ts` — use `ROUTE_PATHS` for static paths and `ROUTE_GENERATORS` for dynamic paths (e.g. `ROUTE_GENERATORS.WAITING_ROOM(roomId)`).

Pages: `EnterPage` (`/`) → `RoomListPage` (`/rooms`) → `WaitingRoom` (`/waiting-room/:roomId`). The latter two are wrapped in `DefaultLayout`.

### WebSocket / STOMP Pattern

The WebSocket client is a singleton in `src/app/config/web-socket-client.ts`. All real-time features go through:

1. `connectWebSocket()` — activates the STOMP client
2. `subscribeManager(destination, callback)` (`stomp-subscribe-manager.ts`) — returns an unsubscribe function
3. `publishMessage(destination, body)` — sends a message
4. `disconnectWebSocket()` — deactivates the client

Domain-specific subscriptions (e.g. `subscribeRoom`, `subscribeError`) wrap `subscribeManager`. Message parsing logic lives in `handle-*-message.ts` files.

The `useRoomSocket` hook manages connection lifecycle for the waiting room: it connects on mount, subscribes, sends `/app/room/confirm-join`, and disconnects + resets the room store on unmount.

### State Management

**Server state** → TanStack Query hooks in `src/hooks/` (e.g. `useRooms`, `useCreateRoom`).

**Global UI state** → Zustand stores in `src/store/`:

- `user-store.ts` — auth state, persisted to localStorage
- `room-store.ts` — current room info + players, reset on leaving the room

Zustand store rules (from `.cursor/rules/zustand.mdc`):

- Middleware wrap order: `devtools → persist → subscribeWithSelector → immer → combine`
- `combine` is required when using TypeScript
- Export one selector hook per state slice (e.g. `useRoomInfo`, `usePlayers`); one hook call = one state value to avoid unnecessary re-renders
- Access the store outside React components via `useXxxStore.getState()`

### API Layer

Each domain under `src/api/` exports:

- An `index.ts` with plain async functions (no React, just Axios calls)
- A `types.ts` for domain types
- `handle-*-message.ts` for WebSocket message handling (reads/writes store via `getState()`)
- `subscribe-*.ts` wrapping `subscribeManager`

### Coding Conventions

From `.cursor/rules/coding-style.mdc`:

- **Naming:** `camelCase` variables/functions, `PascalCase` components/interfaces, `UPPER_SNAKE_CASE` exported constants, `kebab-case` files/directories
- **Booleans:** prefix with `is`, `has`, `should`
- **Types:** prefer `interface` for objects, `type` for unions/intersections
- **Components:** function components with named exports only; props interface named `ComponentNameProps`
- **Comments:** write explanations in Korean; use JSDoc for complex types; explain "why" not "what"
- **Exports:** prefer named exports over default exports (pages use default export)
