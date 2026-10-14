import type { UseQueryResult } from "@tanstack/react-query";
import { useEffect, useRef, type ReactNode } from "react";
import { isApiError } from "../../api/errors";
import { ApiErrorPage } from "./ApiErrorPage";
import { ServerErrorPage } from "./ServerErrorPage";

/**
 * Renders the route's content once its main query has data, and the matching full-page error
 * (E1, E3, E4, E5) while it has none. After recovering from an error, focus goes to the page `h1`.
 */
export function QueryGate<T>({
  query,
  fallback = null,
  children,
}: {
  query: UseQueryResult<T>;
  fallback?: ReactNode;
  children: (data: T) => ReactNode;
}) {
  const recovered = useRef(false);
  useEffect(() => {
    if (query.isError) recovered.current = true;
    else if (query.isSuccess && recovered.current) {
      recovered.current = false;
      document.querySelector<HTMLElement>("main h1")?.focus();
    }
  }, [query.isError, query.isSuccess]);

  if (query.isSuccess) return <>{children(query.data)}</>;
  if (query.isError) {
    const retry = () => query.refetch({ throwOnError: true });
    if (!isApiError(query.error)) {
      console.error(query.error);
      return <ServerErrorPage requestId={null} retry={retry} />;
    }
    return <ApiErrorPage error={query.error} retry={retry} />;
  }
  return <>{fallback}</>;
}
