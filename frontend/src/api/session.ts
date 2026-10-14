import { queryOptions, useQuery } from "@tanstack/react-query";
import { fetchMe, type Me } from "./auth";

/** Query key of the signed-in account; `null` data means guest. */
export const meKey = ["me"] as const;

export const meQuery = queryOptions({
  queryKey: meKey,
  queryFn: fetchMe,
  staleTime: Infinity,
  retry: false,
});

export function useMe() {
  return useQuery(meQuery);
}

export function isOrganizer(me: Me | null | undefined): boolean {
  return !!me?.organizer;
}
