import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { getEvent } from "../../api/events";
import { interpolateNodes } from "../../components/RichText";
import { CardLabel } from "../../components/TicketCard";
import { pickerEventIdOf } from "../../lib/returnTo";
import { readItem } from "../../lib/storage";
import type { LoginReason } from "./reason";
import styles from "./Login.module.css";

const EVENT_NAME_TIMEOUT_MS = 1000;

/** Note "After you sign in" (DOC-44 §6.4): one sentence about where the user goes next. */
export function AfterSignInNote({
  returnTo,
  reason,
}: {
  returnTo: string;
  reason: LoginReason | null;
}) {
  const { t } = useTranslation("auth");
  const eventId = pickerEventIdOf(returnTo);
  const hasSelection = eventId !== null && readItem(`tb.selection.${eventId}`) !== null;
  const eventName = useQuery({
    queryKey: ["event", eventId, "name"],
    queryFn: ({ signal }) =>
      getEvent(eventId!, anySignal(signal, AbortSignal.timeout(EVENT_NAME_TIMEOUT_MS))).then(
        (event) => event.name,
      ),
    enabled: reason !== "signed_out" && hasSelection,
    retry: false,
    staleTime: 5000,
  });

  let sentence;
  if (reason === "signed_out") {
    sentence = t("auth.after.tickets");
  } else if (hasSelection && eventName.data) {
    const name = eventName.data;
    sentence = interpolateNodes((values) => t("auth.after.seats", values), {
      event: <strong>{name}</strong>,
    });
  } else {
    sentence = t("auth.after.page");
  }

  return (
    <div className={styles.note} data-testid="after-sign-in">
      <CardLabel>{t("auth.after.label")}</CardLabel>
      <p>{sentence}</p>
    </div>
  );
}

function anySignal(...signals: AbortSignal[]): AbortSignal {
  return typeof AbortSignal.any === "function"
    ? AbortSignal.any(signals)
    : signals[signals.length - 1]!;
}
