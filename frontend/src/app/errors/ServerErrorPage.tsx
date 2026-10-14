import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, ButtonLink } from "../../components/Button";
import { ErrorPage, RequestIdBox } from "../../components/ErrorPage";

/** E5 (DOC-52): 500 of the route's data, a render error or a second chunk load failure. */
export function ServerErrorPage({
  requestId,
  retry,
}: {
  requestId: string | null;
  retry: () => Promise<unknown> | void;
}) {
  const { t } = useTranslation(["errors", "common"]);
  const [busy, setBusy] = useState(false);

  const onRetry = async () => {
    setBusy(true);
    try {
      await retry();
    } catch {
      // still failing: stay on E5
    } finally {
      setBusy(false);
    }
  };

  return (
    <ErrorPage
      testId="error-500"
      role="alert"
      tone="stamp"
      stamp="500"
      label={t("errors.page.server.label")}
      title={t("errors.page.server.title")}
      body={t("errors.page.server.body")}
      extra={
        requestId ? (
          <RequestIdBox label={t("errors.page.server.requestId")} value={requestId} />
        ) : null
      }
      actions={
        <>
          <Button onClick={onRetry} loading={busy}>
            {t("common.retry")}
          </Button>
          <ButtonLink variant="text" to="/">
            {t("common.nav.toEvents")}
          </ButtonLink>
        </>
      }
      foot={t("errors.page.server.foot")}
    />
  );
}
