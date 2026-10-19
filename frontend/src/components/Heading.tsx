import { useEffect, useRef, type ReactNode } from "react";
import styles from "./Heading.module.css";

/**
 * Page `h1` (Barlow Condensed, uppercase by CSS). Takes focus when `focusKey` changes and
 * `autoFocus` is set, so a state change of the screen is announced (DOC-39 §9).
 */
export function Heading({
  children,
  autoFocus,
  focusKey,
}: {
  children: ReactNode;
  autoFocus?: boolean;
  focusKey?: unknown;
}) {
  const ref = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    if (autoFocus) ref.current?.focus();
  }, [autoFocus, focusKey]);
  return (
    <h1 ref={ref} tabIndex={-1} className={styles.h1}>
      {children}
    </h1>
  );
}
