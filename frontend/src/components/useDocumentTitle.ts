import { useEffect } from "react";
import { APP_NAME } from "../lib/env";

/** Sets `<title>` to "{page} · {APP_NAME}"; error pages also add robots noindex (DOC-52 §2). */
export function useDocumentTitle(title: string, options?: { noindex?: boolean }): void {
  const noindex = options?.noindex ?? false;
  useEffect(() => {
    document.title = title ? `${title} · ${APP_NAME}` : APP_NAME;
  }, [title]);
  useEffect(() => {
    if (!noindex) return;
    const meta = document.createElement("meta");
    meta.name = "robots";
    meta.content = "noindex";
    document.head.appendChild(meta);
    return () => meta.remove();
  }, [noindex]);
}
