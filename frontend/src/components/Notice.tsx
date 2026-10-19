import type { ReactNode } from "react";
import styles from "./Notice.module.css";

// Notice of DOC-39 §7.9: four kinds, each with an icon and a role besides its colour (UXP-08).

export type NoticeKind = "info" | "success" | "warning" | "error";

const DEFAULT_ROLE: Record<NoticeKind, "status" | "alert"> = {
  info: "status",
  success: "status",
  warning: "status",
  error: "alert",
};

export function Notice({
  kind,
  title,
  children,
  action,
  role,
}: {
  kind: NoticeKind;
  title?: ReactNode;
  children?: ReactNode;
  action?: ReactNode;
  role?: "status" | "alert";
}) {
  return (
    <div
      className={`${styles.notice} ${styles[kind]}`}
      role={role ?? DEFAULT_ROLE[kind]}
      data-kind={kind}
    >
      <NoticeIcon kind={kind} />
      <div className={styles.content}>
        {title ? <p className={styles.title}>{title}</p> : null}
        {children ? <div className={styles.body}>{children}</div> : null}
        {action ? <div className={styles.action}>{action}</div> : null}
      </div>
    </div>
  );
}

function NoticeIcon({ kind }: { kind: NoticeKind }) {
  return (
    <svg className={styles.icon} viewBox="0 0 20 20" width="20" height="20" aria-hidden="true">
      {kind === "warning" || kind === "error" ? (
        <>
          <path
            d="M10 2.5 18.5 17h-17Z"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            strokeLinejoin="round"
          />
          <path
            d="M10 8v4.2M10 14.2v.3"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
          />
        </>
      ) : (
        <>
          <circle cx="10" cy="10" r="8" fill="none" stroke="currentColor" strokeWidth="1.6" />
          {kind === "success" ? (
            <path
              d="m6.3 10.2 2.5 2.5 4.9-5.1"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          ) : (
            <path
              d="M10 9v5M10 6.2v.3"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
            />
          )}
        </>
      )}
    </svg>
  );
}
