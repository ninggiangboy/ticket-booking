import type { ReactNode } from "react";
import styles from "./EmptyState.module.css";

/** Empty state: what is missing, one sentence, the next step (UXP-07, DOC-39 §7.17). */
export function EmptyState({
  title,
  body,
  action,
}: {
  title: string;
  body: string;
  action?: ReactNode;
}) {
  return (
    <div className={styles.empty}>
      <h2 className={styles.title}>{title}</h2>
      <p>{body}</p>
      {action ? <div>{action}</div> : null}
    </div>
  );
}
