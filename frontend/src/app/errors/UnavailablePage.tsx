import { useTranslation } from "react-i18next";
import type { ApiError } from "../../api/errors";
import { Button, ButtonLink } from "../../components/Button";
import { ErrorPage } from "../../components/ErrorPage";
import { ProgressBar } from "../../components/ProgressBar";
import { useAutoRetry } from "./useAutoRetry";
import styles from "./UnavailablePage.module.css";

/** E4 (DOC-52): first load of the route's data failed with 503, 429 or no response. */
export function UnavailablePage({
  error,
  retry,
}: {
  error: ApiError;
  retry: () => Promise<unknown>;
}) {
  const { t } = useTranslation(["errors", "common"]);
  const { phase, wait, remaining, lastError, retryNow } = useAutoRetry(error, retry);
  const stamp = lastError.status === null ? "···" : String(lastError.status);

  return (
    <ErrorPage
      testId="error-503"
      role="status"
      tone="warn"
      stamp={stamp}
      label={t("errors.page.busy.label")}
      title={t("errors.page.busy.title")}
      body={t("errors.page.busy.body")}
      extra={
        phase === "stopped" ? null : (
          <div className={styles.retry} data-testid="auto-retry">
            <p className={styles.line}>
              {phase === "running"
                ? t("common.retry.running")
                : t("common.retry.auto", { seconds: remaining })}
            </p>
            <ProgressBar
              label={t("common.retry.running")}
              value={phase === "running" ? 1 : (wait - remaining) / wait}
            />
          </div>
        )
      }
      actions={
        <>
          <Button onClick={retryNow} loading={phase === "running"}>
            {phase === "stopped" ? t("common.retry") : t("common.retryNow")}
          </Button>
          <ButtonLink variant="text" to="/">
            {t("common.nav.toEvents")}
          </ButtonLink>
        </>
      }
      foot={t("errors.page.busy.foot")}
    />
  );
}
