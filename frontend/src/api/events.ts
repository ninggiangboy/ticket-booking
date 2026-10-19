import { api, call } from "./client";
import type { components } from "./schema";

export type EventDetail = components["schemas"]["EventDetail"];

/** E-07. */
export async function getEvent(eventId: string, signal?: AbortSignal): Promise<EventDetail> {
  const { data } = await call(
    api.GET("/events/{eventId}", { params: { path: { eventId } }, signal }),
  );
  return data;
}
