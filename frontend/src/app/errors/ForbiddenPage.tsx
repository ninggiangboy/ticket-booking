import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router";
import { logout } from "../../api/auth";
import { meKey, useMe } from "../../api/session";
import { Button, ButtonLink } from "../../components/Button";
import { ErrorPage } from "../../components/ErrorPage";
import { loginPath } from "../../lib/returnTo";

/** E3 (DOC-52): 403 FORBIDDEN under /studio/** (DR-140). */
export function ForbiddenPage() {
  const { t } = useTranslation(["errors", "common"]);
  const email = useMe().data?.email;
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);

  const signInElsewhere = async () => {
    setBusy(true);
    await logout();
    queryClient.clear();
    queryClient.setQueryData(meKey, null);
    navigate(loginPath("/studio", "signed_out"), { replace: true });
  };

  return (
    <ErrorPage
      testId="error-403"
      role="alert"
      tone="stamp"
      stamp="403"
      label={t("errors.page.forbidden.label")}
      title={t("errors.page.forbidden.title")}
      body={email ? t("errors.page.forbidden.body", { email }) : null}
      actions={
        <>
          <ButtonLink to="/studio">{t("errors.page.forbidden.cta")}</ButtonLink>
          <Button variant="text" loading={busy} onClick={signInElsewhere}>
            {t("errors.page.forbidden.other")}
          </Button>
        </>
      }
      foot={t("errors.page.forbidden.foot")}
    />
  );
}
