import { useTranslation } from "react-i18next";
import { Link, useLocation, useSearchParams } from "react-router";
import { useMe } from "../../api/session";
import { AccountMenu } from "../../features/auth/AccountMenu";
import { LanguageSwitcher } from "../../features/auth/LanguageSwitcher";
import { APP_NAME } from "../../lib/env";
import { eventIdOf, loginPath, sanitizeReturnTo } from "../../lib/returnTo";
import styles from "./Header.module.css";

export type HeaderKind = "default" | "auth" | "error";

/**
 * Shared header (DOC-38 §4): logo, language switch and the account area. The sign-in screens show a
 * back link instead of the account (DOC-44 §4); error pages only read the cached account (DOC-52 §4).
 */
export function Header({ kind }: { kind: HeaderKind }) {
  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <Link to="/" className={styles.logo}>
          {APP_NAME}
          <span className={styles.dot} aria-hidden="true">
            .
          </span>
        </Link>
        <div className={styles.end}>
          <LanguageSwitcher />
          {kind === "auth" ? <BackLink /> : <AccountArea showMyTickets={kind === "error"} />}
        </div>
      </div>
    </header>
  );
}

function BackLink() {
  const { t } = useTranslation(["auth", "common"]);
  const [params] = useSearchParams();
  const eventId = eventIdOf(sanitizeReturnTo(params.get("returnTo")));
  return eventId ? (
    <Link className={styles.link} to={`/events/${eventId}`}>
      {t("auth.event.backToEvent")}
    </Link>
  ) : (
    <Link className={styles.link} to="/">
      {t("common.nav.toEvents")}
    </Link>
  );
}

function AccountArea({ showMyTickets }: { showMyTickets: boolean }) {
  const { t } = useTranslation("common");
  const me = useMe().data;
  const { pathname, search } = useLocation();
  if (me) return <AccountMenu me={me} />;
  if (showMyTickets) {
    return (
      <Link className={styles.link} to={loginPath("/me/tickets")}>
        {t("common.nav.myTickets")}
      </Link>
    );
  }
  return (
    <Link className={styles.link} to={loginPath(pathname + search)}>
      {t("common.nav.signIn")}
    </Link>
  );
}
