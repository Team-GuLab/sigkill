import { roomHandlers } from "@/__mocks__/handlers/rooms";
import { userHandlers } from "@/__mocks__/handlers/users";
import { wsHandlers } from "@/__mocks__/handlers/websocket";

export const handlers = [...roomHandlers, ...userHandlers, ...wsHandlers];
