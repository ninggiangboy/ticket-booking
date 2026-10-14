import type { RouteObject } from "react-router";
import { AuthCallback } from "../features/auth/AuthCallback";
import { Login } from "../features/auth/Login";
import { EventList } from "../features/events/EventList";
import { MyTickets } from "../features/tickets/MyTickets";
import { NotFoundPage } from "./errors/NotFoundPage";
import { RouteErrorBoundary } from "./errors/RouteErrorBoundary";
import { AppLayout } from "./layout/AppLayout";
import { RequireAuth } from "./RequireAuth";

// URL map of DR-67 (DOC-38 §3). Light screens live in the main chunk; heavy ones (seat picker,
// checkout, studio) arrive later as `lazy` routes with their own chunk.

export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    errorElement: <RouteErrorBoundary />,
    children: [
      {
        errorElement: <RouteErrorBoundary />,
        children: [
          { index: true, element: <EventList /> },
          { path: "login", element: <Login />, handle: { header: "auth" } },
          { path: "auth/callback", element: <AuthCallback />, handle: { header: "auth" } },
          {
            path: "me/tickets",
            element: (
              <RequireAuth>
                <MyTickets />
              </RequireAuth>
            ),
          },
          { path: "*", element: <NotFoundPage />, handle: { header: "error" } },
        ],
      },
    ],
  },
];
