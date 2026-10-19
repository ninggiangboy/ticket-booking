import styles from "./Stamp.module.css";

// Stamp of DOC-39 §6: only for final results (error codes, "Paid"). Decorative when the same
// text exists elsewhere.

export function Stamp({ text, tone }: { text: string; tone: "stamp" | "warn" }) {
  return (
    <span className={`${styles.stamp} ${styles[tone]}`} aria-hidden="true">
      {text}
    </span>
  );
}
