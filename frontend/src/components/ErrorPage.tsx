import type { ReactNode } from "react";
import { Heading } from "./Heading";
import { Stamp } from "./Stamp";
import { CardLabel, PaperCard } from "./TicketCard";
import { useDocumentTitle } from "./useDocumentTitle";
import styles from "./ErrorPage.module.css";

// Shared shell of the full-page errors E1, E3, E4, E5 (DOC-39 §7.18, DOC-52 §4).

export function ErrorPage({
  label,
  stamp,
  tone,
  title,
  body,
  extra,
  actions,
  foot,
  role,
  testId,
}: {
  label: string;
  stamp: string;
  tone: "stamp" | "warn";
  title: string;
  body: ReactNode;
  extra?: ReactNode;
  actions: ReactNode;
  foot: ReactNode;
  role: "alert" | "status";
  testId: string;
}) {
  useDocumentTitle(title, { noindex: true });
  return (
    <div className={styles.page}>
      <PaperCard width="wide" role={role} foot={foot} testId={testId}>
        <div className={styles.top}>
          <CardLabel>{label}</CardLabel>
          <Stamp text={stamp} tone={tone} />
        </div>
        <Heading autoFocus>{title}</Heading>
        <p>{body}</p>
        {extra}
        <div className={styles.actions}>{actions}</div>
      </PaperCard>
    </div>
  );
}

/** "Request ID" box of E5: the full 32 hex characters, selectable in one click. */
export function RequestIdBox({ label, value }: { label: string; value: string }) {
  return (
    <div className={styles.requestId}>
      <span className={styles.requestIdLabel}>{label}</span>
      <span className={styles.requestIdValue} data-testid="request-id">
        {value}
      </span>
    </div>
  );
}
