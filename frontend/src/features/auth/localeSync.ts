import type { QueryClient } from "@tanstack/react-query";
import { updateLocale, type Me } from "../../api/auth";
import { isApiError } from "../../api/errors";
import { meKey } from "../../api/session";
import { currentLocale, switchLanguage } from "../../lib/i18n";
import { isLocale, readLocaleCookie, writeLocaleCookie, type Locale } from "../../lib/locale";
import { readItem, removeItem, writeItem } from "../../lib/storage";

// Language changes (FL-04): the interface switches first and never waits for the network; a signed-in
// user's choice is then saved with PATCH /me (debounced 300 ms). A failed save is retried on the next
// page load from `tb.pendingLocale` (UC-20 E1).

const PENDING = "tb.pendingLocale";
const DEBOUNCE_MS = 300;
let saveTimer: number | undefined;

export async function changeLocale(locale: Locale, queryClient: QueryClient): Promise<void> {
  const previous = currentLocale();
  await switchLanguage(locale);
  writeLocaleCookie(locale);
  if (!queryClient.getQueryData<Me | null>(meKey)) return;
  window.clearTimeout(saveTimer);
  saveTimer = window.setTimeout(() => void saveLocale(locale, previous, queryClient), DEBOUNCE_MS);
}

async function saveLocale(locale: Locale, previous: Locale, queryClient: QueryClient) {
  try {
    const me = await updateLocale(locale);
    removeItem(PENDING);
    queryClient.setQueryData(meKey, me);
  } catch (error) {
    if (isApiError(error) && error.status === 422) {
      // A locale the server refuses can only come from a modified client (FL-04 E1).
      console.error("locale rejected", error.code, error.requestId);
      await switchLanguage(previous).catch(() => {});
      writeLocaleCookie(previous);
    } else if (!isApiError(error) || error.status !== 401) {
      writeItem(PENDING, locale);
    }
  }
}

/**
 * Applies the account's locale once the session is known (DOC-31 §2). Right after sign-in the
 * browser's cookie wins and is saved to the account (DR-115); on a normal load the account wins.
 * A save that failed earlier is sent again first.
 */
export async function applyAccountLocale(
  me: Me,
  queryClient: QueryClient,
  options: { afterSignIn: boolean },
): Promise<void> {
  const pending = readItem(PENDING);
  const chosen = isLocale(pending) ? pending : options.afterSignIn ? readLocaleCookie() : null;
  if (chosen) {
    if (chosen !== currentLocale()) await switchLanguage(chosen).catch(() => {});
    if (chosen === me.locale) {
      removeItem(PENDING);
      return;
    }
    try {
      queryClient.setQueryData(meKey, await updateLocale(chosen));
      removeItem(PENDING);
    } catch {
      writeItem(PENDING, chosen);
    }
    return;
  }
  if (me.locale !== currentLocale()) {
    await switchLanguage(me.locale).catch(() => {});
  }
  writeLocaleCookie(me.locale);
}
