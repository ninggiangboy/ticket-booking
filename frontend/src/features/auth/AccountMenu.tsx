import { useQueryClient } from "@tanstack/react-query";
import { useEffect, useId, useRef, useState, type KeyboardEvent } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router";
import { logout, type Me } from "../../api/auth";
import { meKey } from "../../api/session";
import styles from "./AccountMenu.module.css";

/**
 * Account menu (DOC-39 §7.16, DOC-38 §4). Enter/Space opens, Esc closes and returns focus,
 * arrow keys move between items.
 */
export function AccountMenu({ me }: { me: Me }) {
  const { t } = useTranslation("common");
  const [open, setOpen] = useState(false);
  const [signingOut, setSigningOut] = useState(false);
  const menuId = useId();
  const root = useRef<HTMLDivElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const items = () =>
    Array.from(root.current?.querySelectorAll<HTMLElement>("[role=menuitem]") ?? []);

  useEffect(() => {
    if (!open) return;
    items()[0]?.focus();
    const onPointer = (event: PointerEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("pointerdown", onPointer);
    return () => document.removeEventListener("pointerdown", onPointer);
  }, [open]);

  const close = (refocus: boolean) => {
    setOpen(false);
    if (refocus) trigger.current?.focus();
  };

  const onKeyDown = (event: KeyboardEvent) => {
    if (!open) return;
    const list = items();
    const index = list.indexOf(document.activeElement as HTMLElement);
    if (event.key === "Escape") {
      event.preventDefault();
      close(true);
    } else if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      const step = event.key === "ArrowDown" ? 1 : -1;
      list[(index + step + list.length) % list.length]?.focus();
    } else if (event.key === "Home" || event.key === "End") {
      event.preventDefault();
      (event.key === "Home" ? list[0] : list[list.length - 1])?.focus();
    } else if (event.key === "Tab") {
      setOpen(false);
    }
  };

  // FL-03 (a): always ends signed out, even when the request fails.
  const signOut = async () => {
    setSigningOut(true);
    await logout();
    queryClient.clear();
    queryClient.setQueryData(meKey, null);
    setOpen(false);
    navigate("/login?reason=signed_out", { replace: true });
  };

  const initial = me.email.charAt(0).toUpperCase();

  return (
    <div className={styles.root} ref={root} onKeyDown={onKeyDown}>
      <button
        ref={trigger}
        type="button"
        className={styles.trigger}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={menuId}
        onClick={() => setOpen((value) => !value)}
      >
        <span className={styles.avatar} aria-hidden="true">
          {initial}
        </span>
        <span className={styles.triggerLabel}>{t("common.nav.account")}</span>
        <svg className={styles.caret} viewBox="0 0 12 12" width="12" height="12" aria-hidden="true">
          <path d="m2.5 4.5 3.5 3.5 3.5-3.5" fill="none" stroke="currentColor" strokeWidth="1.6" />
        </svg>
      </button>
      {open ? (
        <div className={styles.panel} id={menuId} role="menu" aria-label={t("common.nav.account")}>
          <div className={styles.identity}>
            <span className={styles.signedInAs}>{t("common.nav.signedInAs")}</span>
            <span className={styles.email} data-testid="account-email">
              {me.email}
            </span>
          </div>
          <Link
            role="menuitem"
            className={styles.item}
            to="/me/tickets"
            onClick={() => close(false)}
          >
            {t("common.nav.myTickets")}
          </Link>
          {me.organizer ? (
            <Link role="menuitem" className={styles.item} to="/studio" onClick={() => close(false)}>
              {t("common.nav.studio")}
            </Link>
          ) : (
            <Link
              role="menuitem"
              className={styles.item}
              to="/studio/profile"
              onClick={() => close(false)}
            >
              {t("common.nav.createOrganizer")}
            </Link>
          )}
          <hr className={styles.rule} />
          <button
            role="menuitem"
            type="button"
            className={styles.item}
            onClick={signOut}
            disabled={signingOut}
            aria-busy={signingOut || undefined}
          >
            {t("common.nav.signOut")}
          </button>
        </div>
      ) : null}
    </div>
  );
}
