import { useTranslation } from "react-i18next";
import { EmptyState } from "../../components/EmptyState";
import { Heading } from "../../components/Heading";
import { useDocumentTitle } from "../../components/useDocumentTitle";
import styles from "./EventList.module.css";

/** `/` (DOC-42). Shell only until P2-10 brings E-06; shows the empty state of the list. */
export function EventList() {
  const { t } = useTranslation("events");
  useDocumentTitle(t("events.list.title"));
  return (
    <div className={styles.page}>
      <Heading>{t("events.list.title")}</Heading>
      <EmptyState title={t("events.list.empty.title")} body={t("events.list.empty.body")} />
    </div>
  );
}
