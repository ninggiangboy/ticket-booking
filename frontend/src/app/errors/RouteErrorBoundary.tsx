import { isRouteErrorResponse, useRevalidator, useRouteError } from "react-router";
import { isApiError } from "../../api/errors";
import { ApiErrorPage } from "./ApiErrorPage";
import { isChunkLoadError, reloadOnceForChunkError } from "./chunkReload";
import { NotFoundPage } from "./NotFoundPage";
import { ServerErrorPage } from "./ServerErrorPage";

/** `errorElement` of the routes: loader errors, render errors and lazy chunk failures. */
export function RouteErrorBoundary() {
  const error = useRouteError();
  const revalidator = useRevalidator();

  if (isRouteErrorResponse(error) && error.status === 404) return <NotFoundPage />;
  if (isApiError(error))
    return <ApiErrorPage error={error} retry={() => revalidator.revalidate()} />;
  if (isChunkLoadError(error) && reloadOnceForChunkError()) return null;

  console.error(error);
  return <ServerErrorPage requestId={null} retry={() => window.location.reload()} />;
}
