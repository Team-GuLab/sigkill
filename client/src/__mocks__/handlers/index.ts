import { roomHandlers } from "@/__mocks__/handlers/rooms";
import { wsHandlers } from "@/__mocks__/handlers/websocket";

export const handlers = [...roomHandlers, ...wsHandlers];
