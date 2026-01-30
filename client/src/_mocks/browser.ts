import { setupWorker } from "msw/browser";
import { roomHandlers } from "./handlers/room";

export const worker = setupWorker(...roomHandlers);
