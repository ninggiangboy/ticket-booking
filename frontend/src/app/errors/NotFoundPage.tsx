import { useTranslation } from "react-i18next";
import { useMe } from "../../api/session";
import { ButtonLink } from "../../components/Button";
import { ErrorPage } from "../../components/ErrorPage";
import { loginPath } from "../../lib/returnTo";

/** E1 (DOC-52): unknown route, malformed id or 404 of the route's main data. */
export function NotFoundPage() {
  const { t } = useTranslation(["errors", "common"]);
  const me = useMe().data;
  return (
    <ErrorPage
      testId="error-404"
      role="alert"
      tone="stamp"
      stamp={t("errors.page.notFound.code")}
      label={t("errors.page.notFound.label")}
      title={t("errors.page.notFound.title")}
      body={t("errors.page.notFound.body")}
      actions={
        <>
          <ButtonLink to="/">{t("errors.page.notFound.cta")}</ButtonLink>
          <ButtonLink variant="text" to={me ? "/me/tickets" : loginPath("/me/tickets")}>
            {t("common.nav.myTickets")}
          </ButtonLink>
        </>
      }
      foot={t("errors.page.notFound.foot")}
    />
  );
}
