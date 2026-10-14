import styles from "./ProgressBar.module.css";

/** 4 px bar. Without `value` it is indeterminate (no fake progress, DOC-44 §4). */
export function ProgressBar({ value, label }: { value?: number; label: string }) {
  const indeterminate = value === undefined;
  return (
    <div
      className={styles.track}
      role="progressbar"
      aria-label={label}
      aria-valuemin={indeterminate ? undefined : 0}
      aria-valuemax={indeterminate ? undefined : 100}
      aria-valuenow={indeterminate ? undefined : Math.round(value * 100)}
    >
      <div
        className={indeterminate ? styles.indeterminate : styles.fill}
        style={indeterminate ? undefined : { width: `${Math.min(1, Math.max(0, value)) * 100}%` }}
      />
    </div>
  );
}
