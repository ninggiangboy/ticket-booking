import { authHandlers } from "./auth";
import { eventHandlers } from "./events";

export const handlers = [...authHandlers, ...eventHandlers];
