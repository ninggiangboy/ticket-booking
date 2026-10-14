import { Navigate, useLocation } from "react-router";
import type { ApiError } from "../../api/errors";
import { ForbiddenPage } from "./ForbiddenPage";
import { NotFoundPage } from "./NotFoundPage";
import { ServerErrorPage } from "./ServerErrorPage";
import { UnavailablePage } from "./UnavailablePage";

/**
 * Full-page error for the main data of a route (DOC-52 §2, DOC-40 §1.5). Only used while there is
 * no content to keep; failures over existing content are notices instead.
 */
export function ApiErrorPage({ error, retry }: { error: ApiError; retry: () => Promise<unknown> }) {
  const { pathname } = useLocation();
  if (error.status === 401) return null; // the API client already redirects to sign-in (E2)
  if (error.status === 404) return <NotFoundPage />;
  if (error.status === 403 && error.code === "ORGANIZER_PROFILE_REQUIRED") {
    return <Navigate to="/studio/profile" replace />;
  }
  if (error.status === 403 && error.code === "FORBIDDEN" && pathname.startsWith("/studio")) {
    return <ForbiddenPage />;
  }
  if (error.isTransient) return <UnavailablePage error={error} retry={retry} />;
  return <ServerErrorPage requestId={error.requestId} retry={retry} />;
}
