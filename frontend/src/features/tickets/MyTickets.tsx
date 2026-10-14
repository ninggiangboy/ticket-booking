import { useTranslation } from "react-i18next";
import { ButtonLink } from "../../components/Button";
import { EmptyState } from "../../components/EmptyState";
import { Heading } from "../../components/Heading";
import { useDocumentTitle } from "../../components/useDocumentTitle";
import styles from "./MyTickets.module.css";

/** `/me/tickets` (DOC-50). Shell only until P2-10 brings E-21; shows the empty state. */
export function MyTickets() {
  const { t } = useTranslation("tickets");
  useDocumentTitle(t("tickets.title"));
  return (
    <div className={styles.page}>
      <Heading>{t("tickets.title")}</Heading>
      <EmptyState
        title={t("tickets.empty.title")}
        body={t("tickets.empty.body")}
        action={<ButtonLink to="/">{t("tickets.empty.cta")}</ButtonLink>}
      />
    </div>
  );
}
