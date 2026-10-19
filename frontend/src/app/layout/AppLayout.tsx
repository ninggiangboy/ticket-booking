import { Suspense, useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { Outlet, ScrollRestoration, useLocation, useMatches } from "react-router";
import { Header, type HeaderKind } from "./Header";
import styles from "./AppLayout.module.css";

interface RouteHandle {
  header?: HeaderKind;
}

/** Skip link, header and the single `<main id="main">` of every page (DOC-39 §9). */
export function AppLayout() {
  const matches = useMatches();
  const handle = matches[matches.length - 1]?.handle as RouteHandle | undefined;
  return (
    <>
      <SkipLink />
      <Header kind={handle?.header ?? "default"} />
      <main id="main" className={styles.main}>
        <Suspense fallback={null}>
          <Outlet />
        </Suspense>
      </main>
      <FocusOnNavigate />
      <ScrollRestoration />
    </>
  );
}

function SkipLink() {
  const { t } = useTranslation("common");
  return (
    <a className={styles.skip} href="#main">
      {t("common.nav.skipToContent")}
    </a>
  );
}

/** After a client-side navigation, focus moves to the page `h1` (DOC-39 §9). */
function FocusOnNavigate() {
  const { pathname } = useLocation();
  const first = useRef(true);
  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    const frame = requestAnimationFrame(() => {
      const heading = document.querySelector<HTMLElement>("main h1");
      if (heading && !heading.contains(document.activeElement)) {
        const active = document.activeElement;
        if (
          !active ||
          active === document.body ||
          !document.querySelector("main")?.contains(active)
        ) {
          heading.focus();
        }
      }
    });
    return () => cancelAnimationFrame(frame);
  }, [pathname]);
  return null;
}
