import type { ReactNode } from "react";
import styles from "./TicketCard.module.css";

// Paper card with a tear line (DOC-39 §6): the shell of the sign-in card and the error pages.

export function PaperCard({
  width,
  children,
  foot,
  role,
  testId,
}: {
  width: "narrow" | "wide";
  children: ReactNode;
  foot?: ReactNode;
  role?: "alert" | "status";
  testId?: string;
}) {
  return (
    <section className={`${styles.card} ${styles[width]}`} role={role} data-testid={testId}>
      <div className={styles.body}>{children}</div>
      {foot ? (
        <>
          <TearLine />
          <div className={styles.foot}>{foot}</div>
        </>
      ) : null}
    </section>
  );
}

export function TearLine() {
  return (
    <div className={styles.tear} aria-hidden="true">
      <span className={styles.hole} />
      <span className={styles.dash} />
      <span className={styles.hole} />
    </div>
  );
}

/** Mono uppercase label above a title (DOC-39 §3 "Label"). */
export function CardLabel({ children }: { children: ReactNode }) {
  return <p className={styles.label}>{children}</p>;
}
