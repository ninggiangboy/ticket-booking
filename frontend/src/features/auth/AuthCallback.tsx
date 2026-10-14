import { useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useLocation, useNavigate } from "react-router";
import { verifyMagicLink } from "../../api/auth";
import { ApiError, isApiError } from "../../api/errors";
import { meKey, meQuery } from "../../api/session";
import { Button, ButtonLink } from "../../components/Button";
import { Heading } from "../../components/Heading";
import { ProgressBar } from "../../components/ProgressBar";
import { CardLabel, PaperCard } from "../../components/TicketCard";
import { useDocumentTitle } from "../../components/useDocumentTitle";
import { sanitizeReturnTo } from "../../lib/returnTo";
import { applyAccountLocale } from "./localeSync";
import styles from "./Login.module.css";

// `/auth/callback?token=` (DOC-44 §6.2, FL-02). The token leaves the address bar before the API
// call and POST /auth/verify runs once per token (DR-114).

const TOKEN = /^[A-Za-z0-9_-]{43}$/;

type State = { kind: "verifying" } | { kind: "invalid" } | { kind: "failed"; error: ApiError };

export function AuthCallback() {
  const { t } = useTranslation(["auth", "common", "errors"]);
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [token] = useState(() => new URLSearchParams(location.search).get("token"));
  const valid = token !== null && TOKEN.test(token);
  const [state, setState] = useState<State>(valid ? { kind: "verifying" } : { kind: "invalid" });
  const [attempt, setAttempt] = useState(0);

  useDocumentTitle(
    state.kind === "invalid"
      ? t("auth.invalid.title")
      : state.kind === "failed"
        ? t("errors.generic.title")
        : t("auth.verifying.title"),
  );

  // Keep the token out of history and Referer.
  useEffect(() => {
    if (location.search) navigate("/auth/callback", { replace: true });
    const meta = document.createElement("meta");
    meta.name = "referrer";
    meta.content = "no-referrer";
    document.head.appendChild(meta);
    return () => meta.remove();
    // only on arrival
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!valid) return;
    let active = true;
    (async () => {
      try {
        const result = await verifyMagicLink(token);
        const me = await queryClient.fetchQuery({ ...meQuery, staleTime: 0 });
        if (!me) throw new ApiError({ status: 500, code: "SESSION_NOT_STORED" });
        await applyAccountLocale(me, queryClient, { afterSignIn: true });
        if (active) navigate(sanitizeReturnTo(result.returnTo), { replace: true });
      } catch (cause) {
        if (!active) return;
        const error = isApiError(cause) ? cause : new ApiError({ status: null, cause });
        if (error.status === 401 || error.status === 422) {
          setState({ kind: "invalid" });
        } else {
          if (error.code === "SESSION_NOT_STORED") {
            console.error("session cookie was not stored after verify");
            queryClient.setQueryData(meKey, null);
          }
          setState({ kind: "failed", error });
        }
      }
    })();
    return () => {
      active = false;
    };
  }, [valid, token, attempt, queryClient, navigate]);

  if (state.kind === "invalid") {
    return (
      <div className={styles.page}>
        <PaperCard width="narrow" testId="login-invalid">
          <CardLabel>{t("auth.invalid.label")}</CardLabel>
          <Heading autoFocus>{t("auth.invalid.title")}</Heading>
          <p>{t("auth.invalid.body")}</p>
          <ButtonLink to="/login" block>
            {t("auth.invalid.cta")}
          </ButtonLink>
        </PaperCard>
      </div>
    );
  }

  if (state.kind === "failed") {
    const rateLimited = state.error.status === 429;
    return (
      <div className={styles.page}>
        <PaperCard width="narrow" role="alert" testId="login-verify-failed">
          <CardLabel>{t("auth.verifying.label")}</CardLabel>
          <Heading autoFocus>
            {rateLimited ? t("errors.rate_limited.title") : t("errors.generic.title")}
          </Heading>
          <p>
            {rateLimited
              ? t("errors.rate_limited.body", { seconds: state.error.retryAfterSeconds ?? 1 })
              : t("errors.generic.body")}
          </p>
          {state.error.requestId && !rateLimited ? (
            <code className={styles.requestId}>{state.error.requestId}</code>
          ) : null}
          <Button
            block
            onClick={() => {
              setState({ kind: "verifying" });
              setAttempt((value) => value + 1);
            }}
          >
            {t("common.retry")}
          </Button>
        </PaperCard>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <PaperCard width="narrow" testId="login-verifying">
        <CardLabel>{t("auth.verifying.label")}</CardLabel>
        <Heading autoFocus>{t("auth.verifying.title")}</Heading>
        <p>{t("auth.verifying.body")}</p>
        <div role="status">
          <ProgressBar label={t("auth.verifying.title")} />
        </div>
      </PaperCard>
    </div>
  );
}
