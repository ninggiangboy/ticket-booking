import type { AnchorHTMLAttributes, ButtonHTMLAttributes, ReactNode } from "react";
import { Link, type LinkProps } from "react-router";
import styles from "./Button.module.css";

// Button variants of DOC-39 §7.1. One `primary` per screen (UXP-01); `urgent` only for
// the last two minutes of a hold or irreversible actions.

export type ButtonVariant = "primary" | "primary-compact" | "secondary" | "text" | "urgent";

interface Common {
  variant?: ButtonVariant;
  block?: boolean;
  children: ReactNode;
}

function classes(variant: ButtonVariant, block: boolean | undefined, extra?: string): string {
  return [styles.button, styles[variant], block ? styles.block : "", extra ?? ""]
    .filter(Boolean)
    .join(" ");
}

export function Button({
  variant = "primary",
  block,
  loading,
  className,
  disabled,
  children,
  type = "button",
  ...rest
}: Common & ButtonHTMLAttributes<HTMLButtonElement> & { loading?: boolean }) {
  return (
    <button
      {...rest}
      type={type}
      className={classes(variant, block, className)}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      data-variant={variant}
    >
      {children}
    </button>
  );
}

export function ButtonLink({
  variant = "primary",
  block,
  className,
  children,
  ...rest
}: Common & LinkProps) {
  return (
    <Link {...rest} className={classes(variant, block, className)} data-variant={variant}>
      {children}
    </Link>
  );
}

/** External link styled as a button; always opens a new tab without opener (LGN-08). */
export function ButtonAnchor({
  variant = "primary",
  block,
  className,
  children,
  ...rest
}: Common & AnchorHTMLAttributes<HTMLAnchorElement>) {
  return (
    <a
      {...rest}
      target="_blank"
      rel="noopener noreferrer"
      className={classes(variant, block, className)}
      data-variant={variant}
    >
      {children}
    </a>
  );
}
