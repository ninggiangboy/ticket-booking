import { useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Navigate, useLocation, useNavigate, useSearchParams } from "react-router";
import { requestMagicLink } from "../../api/auth";
import { isApiError } from "../../api/errors";
import { meQuery, useMe } from "../../api/session";
import { Button, ButtonAnchor } from "../../components/Button";
import { Heading } from "../../components/Heading";
import { Notice } from "../../components/Notice";
import { interpolateNodes } from "../../components/RichText";
import { TextField } from "../../components/TextField";
import { CardLabel, PaperCard } from "../../components/TicketCard";
import { useDocumentTitle } from "../../components/useDocumentTitle";
import { MAILPIT_URL } from "../../lib/env";
import { currentLocale } from "../../lib/i18n";
import { sanitizeReturnTo } from "../../lib/returnTo";
import { AfterSignInNote } from "./AfterSignInNote";
import { isValidEmail, normalizeEmail, webmailUrl } from "./email";
import { parseReason } from "./reason";
import styles from "./Login.module.css";

// Sign-in screen `/login` (DOC-44, FL-01). Variants: form, sent, throttled, signed out and
// session expired; "verifying" and "expired link" live in AuthCallback with the same card.

type Step = { kind: "form" } | { kind: "sent"; email: string; throttled: boolean };

type Failure =
  | { kind: "provider" }
  | { kind: "rateLimited"; until: number }
  | { kind: "generic"; requestId: string | null };

const ME_RECHECK_MS = 5000;

export function Login() {
  const { t } = useTranslation(["auth", "common", "errors"]);
  const [params] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const me = useMe();

  const returnTo = sanitizeReturnTo(params.get("returnTo"));
  const reason = parseReason(params.get("reason"), location.state);

  const [step, setStep] = useState<Step>({ kind: "form" });
  const [email, setEmail] = useState("");
  const [fieldError, setFieldError] = useState(false);
  const [failure, setFailure] = useState<Failure | null>(null);
  const [sending, setSending] = useState(false);
  const [now, setNow] = useState(() => Date.now());
  const input = useRef<HTMLInputElement>(null);
  const [focusInput, setFocusInput] = useState(reason === null ? 1 : 0);

  useDocumentTitle(step.kind === "sent" ? t("auth.sent.title") : t("auth.form.title"));

  useEffect(() => {
    if (focusInput > 0) input.current?.focus();
  }, [focusInput]);

  // 429 countdown: the button stays disabled until it ends; nothing is resent on its own.
  const waitSeconds =
    failure?.kind === "rateLimited" ? Math.max(0, Math.ceil((failure.until - now) / 1000)) : 0;
  useEffect(() => {
    if (failure?.kind !== "rateLimited") return;
    const timer = window.setInterval(() => {
      const time = Date.now();
      setNow(time);
      if (time >= failure.until) setFailure(null);
    }, 250);
    return () => window.clearInterval(timer);
  }, [failure]);

  // Another tab may finish signing in while this one shows "sent" (DOC-44 §6.3, DR-138).
  useEffect(() => {
    if (step.kind !== "sent") return;
    let lastCheck = 0;
    const recheck = () => {
      if (document.hidden || Date.now() - lastCheck < ME_RECHECK_MS) return;
      lastCheck = Date.now();
      queryClient
        .fetchQuery({ ...meQuery, staleTime: 0 })
        .then((user) => {
          if (user) navigate(returnTo, { replace: true });
        })
        .catch(() => {});
    };
    document.addEventListener("visibilitychange", recheck);
    window.addEventListener("focus", recheck);
    return () => {
      document.removeEventListener("visibilitychange", recheck);
      window.removeEventListener("focus", recheck);
    };
  }, [step.kind, queryClient, navigate, returnTo]);

  if (me.data) return <Navigate to={returnTo} replace />;

  const send = async (address: string) => {
    setSending(true);
    setFailure(null);
    try {
      const result = await requestMagicLink({ email: address, returnTo, locale: currentLocale() });
      setStep({ kind: "sent", email: result.email || address, throttled: result.throttled });
    } catch (error) {
      if (!isApiError(error)) throw error;
      if (error.status === 422 && step.kind === "form") {
        setFieldError(true);
        setFocusInput((value) => value + 1);
      } else if (error.code === "EMAIL_PROVIDER_UNAVAILABLE") {
        setFailure({ kind: "provider" });
      } else if (error.status === 429) {
        setNow(Date.now());
        setFailure({
          kind: "rateLimited",
          until: Date.now() + (error.retryAfterSeconds ?? 1) * 1000,
        });
      } else {
        setFailure({ kind: "generic", requestId: error.requestId });
      }
    } finally {
      setSending(false);
    }
  };

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (sending || waitSeconds > 0) return;
    const normalized = normalizeEmail(email);
    if (!isValidEmail(normalized)) {
      setFieldError(true);
      setFocusInput((value) => value + 1);
      return;
    }
    void send(normalized);
  };

  const useOtherEmail = () => {
    setStep({ kind: "form" });
    setEmail("");
    setFieldError(false);
    setFailure(null);
    setFocusInput((value) => value + 1);
  };

  const failureNotice = failure ? (
    failure.kind === "provider" ? (
      <Notice kind="error" title={t("errors.email_provider_unavailable.title")}>
        {t("errors.email_provider_unavailable.body")}
      </Notice>
    ) : failure.kind === "rateLimited" ? (
      <Notice kind="warning" title={t("errors.rate_limited.title")}>
        {t("errors.rate_limited.body", { seconds: waitSeconds })}
      </Notice>
    ) : (
      <Notice kind="error" title={t("errors.generic.title")}>
        {t("errors.generic.body")}
        {failure.requestId ? <code className={styles.requestId}>{failure.requestId}</code> : null}
      </Notice>
    )
  ) : null;

  const foot = <AfterSignInNote returnTo={returnTo} reason={reason} />;

  if (step.kind === "sent") {
    const inbox = webmailUrl(step.email, MAILPIT_URL);
    return (
      <div className={styles.page}>
        <PaperCard width="narrow" foot={foot} testId="login-sent">
          <CardLabel>{t("auth.sent.label")}</CardLabel>
          <Heading autoFocus focusKey={step}>
            {t("auth.sent.title")}
          </Heading>
          <p data-testid="sent-body">
            {interpolateNodes((values) => t("auth.sent.body", values), {
              email: (
                <strong className={styles.email}>
                  {step.email || t("auth.sent.emailFallback")}
                </strong>
              ),
            })}
          </p>
          <p className={styles.hint}>{t("auth.sent.hint")}</p>
          {step.throttled ? (
            <Notice kind="error" role="status" title={t("auth.throttled.title")}>
              {t("auth.throttled.body")}
            </Notice>
          ) : null}
          {failureNotice}
          {inbox ? (
            <ButtonAnchor href={inbox} block>
              {t("auth.sent.openMail")}
            </ButtonAnchor>
          ) : null}
          <div className={styles.secondary}>
            <Button
              variant="text"
              loading={sending}
              disabled={waitSeconds > 0}
              onClick={() => void send(step.email)}
            >
              {t("auth.sent.resend")}
            </Button>
            <Button variant="text" onClick={useOtherEmail}>
              {t("auth.sent.otherEmail")}
            </Button>
          </div>
        </PaperCard>
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <PaperCard width="narrow" foot={foot} testId="login-form">
        {reason === "signed_out" ? (
          <Notice kind="success">{t("auth.signedOut.notice")}</Notice>
        ) : reason === "session_expired" ? (
          <Notice kind="warning" role="alert">
            {t("auth.sessionOver.notice")}
          </Notice>
        ) : null}
        <CardLabel>
          {reason === "signed_out"
            ? t("auth.signedOut.label")
            : reason === "session_expired"
              ? t("auth.sessionOver.label")
              : t("auth.form.label")}
        </CardLabel>
        <Heading autoFocus={reason !== null}>
          {reason ? t("auth.form.title.again") : t("auth.form.title")}
        </Heading>
        <p>{t("auth.form.intro")}</p>
        <form className={styles.form} noValidate onSubmit={onSubmit}>
          <TextField
            ref={input}
            label={t("auth.form.email.label")}
            name="email"
            type="text"
            inputMode="email"
            autoComplete="email"
            autoCapitalize="none"
            spellCheck={false}
            placeholder={t("auth.form.email.placeholder")}
            value={email}
            readOnly={sending}
            error={fieldError ? t("auth.form.email.error") : null}
            onChange={(event) => {
              setEmail(event.target.value);
              setFieldError(false);
            }}
          />
          {failureNotice}
          <Button type="submit" block loading={sending} disabled={waitSeconds > 0}>
            {t("auth.form.submit")}
          </Button>
        </form>
      </PaperCard>
    </div>
  );
}
